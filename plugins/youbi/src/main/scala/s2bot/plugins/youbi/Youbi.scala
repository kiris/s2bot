package s2bot.plugins.youbi

import akka.actor.ActorSystem
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.plugins.youbi.Youbi.YOUBI_PATTERN
import s2bot.{S2Bot, Script}

import scala.concurrent.ExecutionContext

class Youbi(
    mondayEmoji: String = "getsu",
    tuesdayEmoji: String = "ka",
    wednesdayEmoji: String = "sui",
    thursdayEmoji: String = "moku",
    fridayEmoji: String = "kin"
)(implicit system: ActorSystem) extends Script with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def apply(bot: S2Bot): Unit = {
    bot.hear {
      case (YOUBI_PATTERN(), msg) =>
        for {
          _ <- bot.reaction(msg, mondayEmoji)
          _ <- bot.reaction(msg, tuesdayEmoji)
          _ <- bot.reaction(msg, wednesdayEmoji)
          _ <- bot.reaction(msg, thursdayEmoji)
          _ <- bot.reaction(msg, fridayEmoji)
        } yield ()
    }
  }

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"youbi - 曜日調整用のリアクションを付けます",

    )
  )
}

object Youbi {
  val YOUBI_PATTERN = "youbi.*".r
}
