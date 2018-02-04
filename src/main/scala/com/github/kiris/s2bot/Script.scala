package com.github.kiris.s2bot

trait Script {
  def apply(bot: S2Bot): Unit
}