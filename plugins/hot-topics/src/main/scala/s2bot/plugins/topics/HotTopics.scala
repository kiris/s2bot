package s2bot.plugins.topics


import akka.actor.ActorSystem
import s2bot.{Fmt, S2Bot, Script}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import slack.models.{ReactionAdded, ReactionItemMessage}

import scala.concurrent.{ExecutionContext, Future}

class HotTopics(channelName: String = "hot-topics")(implicit system: ActorSystem) extends Script with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.CHANNELS -> List(
      s"${Fmt.linkChannelForName(bot, channelName)} - 沢山のemojiが付いたメッセージを通知します"
    )
  )

  override def apply(bot: S2Bot): Unit =
    bot.onEvent {
      case ReactionAdded(_, ReactionItemMessage(channelId, ts), _, _, _) =>
        for {
          hotTopic <- isHotTopic(bot, channelId, ts)
          _<- bot.getChannelIdForName(channelName) match {
            case Some(hotTopicChannelId) if hotTopic =>
              bot.say(hotTopicChannelId, s"${Fmt.linkMessageUrl(bot, channelId, ts)} が盛り上ってるよ")

            case _ =>
              Future.unit
          }
        } yield ()
    }

  private def isHotTopic(bot: S2Bot, channelId: String, ts: String): Future[Boolean] =
    bot.web.getReactionsForMessage(channelId, ts).map(_.map(_.count).sum == 10)
}
