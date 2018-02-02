package com.github.kiris.s2bot

trait Script {
  def apply(bot: S2Bot): Unit
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
