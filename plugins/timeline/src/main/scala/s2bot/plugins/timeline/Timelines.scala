package s2bot.plugins.timeline


import akka.actor.ActorSystem
import play.api.libs.json
import play.api.libs.json.Json
import s2bot.extensions.brain.Brain._
import s2bot.extensions.brain.{Brain, Codec}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.plugins.timeline.Timelines.Data
import s2bot.{Fmt, Plugin, S2Bot}
import slack.models.{Attachment, Channel, User}

import scala.concurrent.{ExecutionContext, Future}

class Timelines[A : Brain : ({type F[X] = Codec[Data,X]})#F](implicit system: ActorSystem) extends Plugin with Helpable {
  import Timelines._

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"timeline list - 現在のチャンネルが購読しているチャンネルの一覧を表示します",
      s"timeline subscribe <channel> - 現在のチャンネルで <channel> を購読します",
      s"timeline unsubscribe <channel> - 現在のチャンネルで <channel> の購読をやめます"
    )
  )

  override def apply(bot: S2Bot): S2Bot = {
    bot.hear {
      case ("timeline list", msg) =>
        listSubscribed(bot, msg.channel)
      case (SUBSCRIBE_CHANNEL_PATTERN(channelId), msg) =>
        for {
          _ <- subscribe(bot, msg.channel, channelId)
          _ <- bot.say(msg, s"${Fmt.linkChannel(channelId)} を購読するよ")
        } yield ()
      case (SUBSCRIBE_REGEX_PATTERN(pattern), msg) =>
        for {
          _ <- subscribe(bot, msg.channel, pattern)
          _ <- bot.say(msg, s"#$pattern を購読するよ")
        } yield ()
      case (UNSUBSCRIBE_CHANNEL_PATTERN(channelId), msg) =>
        for {
          _ <- unSubscribe(bot, msg.channel, channelId)
          _ <- bot.say(msg, s"${Fmt.linkChannel(channelId)} の購読をやめるよ")
        } yield ()
      case (UNSUBSCRIBE_REGEX_PATTERN(pattern), msg) =>
        for {
          _ <- unSubscribe(bot, msg.channel, pattern)
          _ <- bot.say(msg, s"#$pattern の購読をやめるよ")
        } yield ()
      case (text, message) =>
        message.user.map { userId =>
          for {
            channel <- bot.getChannel(message.channel)
            user <- bot.getUser(userId)
            _ <- postTimelines(bot, text, channel, user, message.attachments)
          } yield ()
        }.getOrElse(Future.unit)
    }
  }

  private def listSubscribed(bot: S2Bot, channelId: String): Future[Unit] = {
    for {
      timelines <- loadData(bot)
      subscribed = timelines.getTimeline(channelId).subscribed.toSeq
      _ <- subscribed match {
        case Nil =>
          bot.say(channelId, "現在購読しているチャンネルは無いよ")
        case subscribed =>
          val listString = subscribed.mkString(" ")
          bot.say(channelId, s"現在購読しているチャンネルは $listString だよ")
      }
    } yield ()
  }

  private def subscribe(bot: S2Bot, channelId: String, pattern: String): Future[Any] =
    synchronized { // XXX
      for {
        timelines <- loadData(bot)
        _ <- saveData(bot, timelines.modifyTimeline(channelId)(_.subscribe(pattern)))
      } yield ()
    }.recover {
      case ex =>
        ex.printStackTrace()
        ex
    }

  private def unSubscribe(bot: S2Bot, channelId: String, pattern: String): Future[Any] =
    synchronized { // XXX
      for {
        timelines <- loadData(bot)
        _ <- saveData(bot, timelines.modifyTimeline(channelId)(_.unSubscribe(pattern)))
      } yield ()
    }

  private def postTimelines(bot: S2Bot, text: String, channel: Channel, user: User, attachments: Option[Seq[Attachment]]): Future[Unit] = {
    if (user.id == bot.self.id) {
      return Future.unit
    }
    for {
      escapedText1 <- {
        val regex = "<@([^>]+)>".r
        regex.findAllIn(text).matchData.foldLeft(Future.successful(text)) { (result, m) =>
          val link = m.group(0)
          val userId = m.group(1)

          for {
            r <- result
            user <- bot.getUser(userId)
          } yield r.replaceFirst(link, s"`${user.name}`")
        }
      }
      escapedText2 = {
        val regex2 = "<!(here|channel|everyone)>".r
        regex2.findAllIn(escapedText1).matchData.foldLeft(escapedText1) { (result, m) =>
          result.replaceFirst(m.group(0), s"`${m.group(1)}`")
        }
      }
      timelines <- loadData(bot)
      _ <- Future.sequence(
        for {
          subscribed <- timelines.getSubscribes(channel)
        } yield bot.web.postChatMessage(
          channelId = subscribed.channelId,
          text = s"$escapedText2 (${Fmt.linkChannel(channel.id)})",
          username = Some(user.name),
          iconUrl = user.profile.map(_.image_48),
          attachments = attachments
        )
      )
    } yield ()
  }

  private def loadData(bot: S2Bot): Future[Data] = bot.brain.get[Data](BRAIN_KEY).map(_.getOrElse(Data.empty))

  private def saveData(bot: S2Bot, data: Data): Future[Boolean] = bot.brain.set(BRAIN_KEY, data)
}

object Timelines {
  val BRAIN_KEY = "timeline"

  val SUBSCRIBE_CHANNEL_PATTERN = "timeline subscribe <#(.+)\\|.*>".r

  val SUBSCRIBE_REGEX_PATTERN = "timeline subscribe #?(.+)".r

  val UNSUBSCRIBE_CHANNEL_PATTERN = "timeline unsubscribe <#(.+)\\|.*>".r

  val UNSUBSCRIBE_REGEX_PATTERN = "timeline unsubscribe #?(.+)".r

  case class Data(timelines: Map[String, Timeline]) {
    def getTimeline(channelId: String): Timeline = timelines.getOrElse(channelId, Timeline.empty(channelId))
    def getSubscribes(channel: Channel): List[Timeline] = timelines.values.filter(_.isSubscribe(channel)).toList
    def modifyTimeline(channelId: String)(f: Timeline => Timeline) = {
      this.copy(
        timelines = timelines + (channelId -> f(getTimeline(channelId)))
      )
    }
  }

  object Data {
    val empty: Data = Data(timelines = Map.empty)
  }

  case class Timeline(channelId: String, subscribed: Set[String] = Set.empty, ignored: Set[String] = Set.empty) {
    def isSubscribe(channel: Channel): Boolean = subscribed.contains(channel.id) || subscribed.exists(channel.name.matches)
    def subscribe(pattern: String): Timeline = this.copy(subscribed = subscribed + pattern)
    def unSubscribe(pattern: String): Timeline = this.copy(subscribed = subscribed - pattern)
    def ignore(pattern: String): Timeline = this.copy(ignored = ignored + pattern)
    def unIgnore(pattern: String): Timeline = this.copy(ignored = ignored - pattern)
  }

  object Timeline {
    def empty(channelId: String): Timeline = Timeline(channelId)
  }

  // for json
  implicit val timelineFormat: json.Format[Timeline] = Json.using[Json.WithDefaultValues].format[Timeline]

  implicit val dataFormat: json.Format[Data] = Json.using[Json.WithDefaultValues].format[Data]

}