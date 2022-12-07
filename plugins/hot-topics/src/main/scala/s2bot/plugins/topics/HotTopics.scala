package s2bot.plugins.topics


import akka.actor.ActorSystem
import s2bot.{Fmt, S2Bot, Plugin}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import slack.models.{ReactionAdded, ReactionItemMessage}

import scala.concurrent.{ExecutionContext, Future}

class HotTopics(channelName: String = "hot-topics", threshold: Int = 10)(implicit system: ActorSystem) extends Plugin with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.CHANNELS -> List(
      s"#$channelName - 沢山のemojiが付いたメッセージを通知します"
    )
  )

  override def apply(bot: S2Bot): S2Bot =
    bot.onEvent {
      case ReactionAdded(_, ReactionItemMessage(channelId, ts), _, _, _) =>
        for {
          hotTopic <- isHotTopic(bot, channelId, ts)
          hotTopicChannelIdOpt <- bot.getChannelIdForName(channelName)
          _<- hotTopicChannelIdOpt match {
            case Some(hotTopicChannelId) if hotTopic =>
              bot.say(hotTopicChannelId, s"${Fmt.linkMessageUrl(bot, channelId, ts)} が盛り上ってるよ")

            case _ =>
              Future.unit
          }
        } yield ()
    }

  private def isHotTopic(bot: S2Bot, channelId: String, ts: String): Future[Boolean] =
    bot.web.getReactionsForMessage(channelId, ts).map(_.map(_.count).sum == threshold)
}
