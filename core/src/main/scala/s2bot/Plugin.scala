package s2bot

trait Plugin {
  def apply(bot: S2Bot): S2Bot
}
