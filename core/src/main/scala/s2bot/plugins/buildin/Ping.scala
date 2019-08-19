package s2bot.plugins.buildin

import s2bot.plugins.buildin.Helpable.{DefaultKeys, Usage}
import s2bot.{S2Bot, Plugin}
import slack.models.Message

object Ping extends Plugin with Helpable {

  override def apply(bot: S2Bot): S2Bot = {
    bot.respond {
      case ("ping", message) =>
        bot.reply(message, responsePong(message))
    }
  }

  protected def responsePong(message: Message): String = "pong"

  override def usage(bot: S2Bot): Usage = Usage(
    DefaultKeys.COMMANDS -> List(
      s"${bot.me} ping - pong"
    )
  )

}
