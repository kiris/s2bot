package com.github.kiris.s2bot.buildin

import com.github.kiris.s2bot.{S2Bot, Script}

import scala.util.Success

object Help extends Script with Helpable {
  override def usage(): Usage = Usage(
    commands = List(
      "@me help - print this message."
    )
  )

  def apply(bot: S2Bot): Unit = {
    bot.respond {
      case ("help", message) =>
        val usage = bot.scripts.collect {
          case h: Helpable => h.usage()
        }.foldLeft(Usage.empty)(_ + _)
        
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

trait Helpable {
  def usage(): Usage
}

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
