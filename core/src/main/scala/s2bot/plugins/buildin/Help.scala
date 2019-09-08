package s2bot.plugins.buildin

import cats.implicits._
import Helpable.DefaultKeys
import s2bot.{S2Bot, Plugin}

object Help extends Plugin with Helpable {
  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"${bot.me} help - print this message."
    )
  )

  def apply(bot: S2Bot): S2Bot = {
    bot.respond {
      case (HELP_PATTERN, message) =>
        val usages = bot.plugins.collect {
          case h: Helpable => h.usage(bot)
        }.foldLeft(Helpable.Usage.empty)(_ + _)

        bot.say(message, responseHelp(usages))
    }
  }

  protected def responseHelp(usages: Helpable.Usage): String =
    usages.values.map { case (key, value) =>
      s"""
         |*<$key>*
         |${value.map("* " + _).mkString("\n")}""".stripMargin
    }.mkString("\n\n")

  protected val HELP_PATTERN: String = "help"
}


trait Helpable {
  def usage(bot: S2Bot): Helpable.Usage
}

object Helpable {
  case class Usage(values: Map[String, List[String]]) {
    def +(that: Usage): Usage =
      Usage(
        values = this.values combine that.values
      )
  }

  object Usage {
    val empty: Usage = Usage()

    def apply(usages: (String, List[String])*): Usage = Usage(Map(usages: _*))
  }

  object DefaultKeys {
    val COMMANDS = "commands"
    val JOBS = "jobs"
    val CHANNELS = "channels"
    val ACTIONS = "actions"
    val REACTIONS = "reactions"


    def commands(args: String*): (String, List[String]) = COMMANDS -> args.toList

    def jobs(args: String*): (String, List[String]) = JOBS -> args.toList

    def channels(args: String*): (String, List[String]) = CHANNELS -> args.toList

    def actions(args: String*): (String, List[String]) = ACTIONS -> args.toList

    def reactions(args: String*): (String, List[String]) = REACTIONS -> args.toList
  }
}