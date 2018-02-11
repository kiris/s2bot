package com.github.kiris.s2bot.buildin

import com.github.kiris.s2bot.{S2Bot, Script}

object Help extends Script with Helpable {

  override def usage(): Helpable.Usage = Helpable.Usage(
    commands = List(
      "@me help - print this message."
    )
  )

  def apply(bot: S2Bot): Unit = {
    bot.respond {
      case ("help", message) =>
        val usage = bot.scripts.collect {
          case h: Helpable => h.usage()
        }.foldLeft(Helpable.Usage.empty)(_ + _)

        bot.say(message,
          s"""*<commands>*
             |${usage.commands.map("* " + _).mkString("\n")}
             |
             |*<jobs>*
             |${usage.jobs.map("* " + _).mkString("\n")}
             |""".stripMargin.replaceAll("@me", "@noboru"))
    }
  }
}


object Helpable {

  case class Usage(
      commands: List[String] = Nil,
      jobs: List[String] = Nil
  ) {
    def +(that: Usage): Usage =
      Usage(
        commands = commands ++ that.commands,
        jobs = jobs ++ that.jobs
      )
  }

  object Usage {
    val empty: Usage = Usage(Nil, Nil)
  }

}

trait Helpable {
  def usage(): Helpable.Usage
}
