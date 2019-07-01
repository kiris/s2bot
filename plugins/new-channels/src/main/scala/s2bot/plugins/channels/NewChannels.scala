package s2bot.plugins.channels

import com.typesafe.config.Config
import s2bot.{Fmt, S2Bot, Script}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import slack.models.ChannelCreated

import scala.concurrent.Future

class NewChannels(channelName: String = "new-channels") extends Script with Helpable {

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.CHANNELS -> List(
      s"${Fmt.linkChannelForName(bot, channelName)} - 新しいチャンネルが作られら通知します"
    )
  )

  override def apply(bot: S2Bot): Unit =
    bot.onEvent {
      case (ChannelCreated(channel)) =>
        bot.getChannelIdForName(channelName) match {
          case Some(channelId) => bot.say(channelId, s"新しいチャンネル ${Fmt.linkChannel(channel)} が作られたよ")
          case None => Future.successful(())
        }
    }
}

object NewChannels {
  def apply(config: Config): NewChannels = new NewChannels(
    channelName = config.getString("s2bot.plugins.newChannels.channelName")
  )
}