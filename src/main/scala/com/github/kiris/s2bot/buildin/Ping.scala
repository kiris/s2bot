package com.github.kiris.s2bot.buildin

import com.github.kiris.s2bot.buildin.Help.Usage
import com.github.kiris.s2bot.{S2Bot, Script}

object Ping extends Script with Helpable {

  override def apply(bot: S2Bot): Unit = {
    bot.respond { case ("ping", message) =>
      bot.reply(message, "pong").value
    }
  }

  override def usage(): Usage = Usage(
    commands = List(
      "@me ping - pong"
    )
  )

}
