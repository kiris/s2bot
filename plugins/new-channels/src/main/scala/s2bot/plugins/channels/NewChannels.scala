package s2bot.plugins.channels

import akka.actor.ActorSystem
import com.typesafe.config.Config
import s2bot.{Fmt, Plugin, S2Bot}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import slack.models.ChannelCreated

import scala.concurrent.{ExecutionContext, Future}

class NewChannels(channelName: String = "new-channels")(implicit system: ActorSystem) extends Plugin with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.CHANNELS -> List(
      s"#$channelName - 新しいチャンネルが作られたら通知します"
    )
  )

  override def apply(bot: S2Bot): S2Bot =
    bot.onEvent {
      case ChannelCreated(channel) =>
        for {
          channelId <- bot.getChannelIdForName(channelName)
          _ <- channelId match {
            case Some(channelId) => bot.say(channelId, s"新しいチャンネル ${Fmt.linkChannel(channel)} が作られたよ")
            case None            => Future.unit
          }
        } yield ()
    }
}

object NewChannels {
  def apply(config: Config)(implicit system: ActorSystem): NewChannels = new NewChannels(
    channelName = config.getString("s2bot.plugins.newChannels.channelName")
  )
}