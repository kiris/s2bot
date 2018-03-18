package com.github.kiris.s2bot.buildin

import com.github.kiris.s2bot.buildin.Helpable.Usage
import com.github.kiris.s2bot.{S2Bot, Script}

object Ping extends Script with Helpable {

  override def apply(bot: S2Bot): Unit = {
    bot.respond {
      case ("ping", message) =>
        bot.reply(message, "pong")
    }
  }

  override def usage(bot: S2Bot): Usage = Usage(
    commands = List(
      s"${bot.me} ping - pong"
    )
  )

}
