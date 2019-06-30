package s2bot.plugins.buildin

import s2bot.plugins.buildin.Helpable.{DefaultKeys, Usage}
import s2bot.{S2Bot, Script}
import slack.models.Message

object Ping extends Script with Helpable {

  override def apply(bot: S2Bot): Unit = {
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
