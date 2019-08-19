package s2bot.plugins.buildin

import s2bot.plugins.buildin.Helpable.{DefaultKeys, Usage}
import s2bot.{S2Bot, Plugin}
import slack.models.Message

import scala.concurrent.ExecutionContext

class Exit(implicit ec: ExecutionContext) extends Plugin with Helpable {

  override def apply(bot: S2Bot): S2Bot = {
    bot.respond {
      case ("exit", message) =>
        for {
          _ <- bot.reply(message, responsePong(message))
        } yield System.exit(1)
    }
  }

  protected def responsePong(message: Message): String = "bye bye"

  override def usage(bot: S2Bot): Usage = Usage(
    DefaultKeys.COMMANDS -> List(
      s"${bot.me} exit - プロセスを終了します"
    )
  )
}
