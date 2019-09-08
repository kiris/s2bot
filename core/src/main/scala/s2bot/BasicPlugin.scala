package s2bot

import s2bot.S2Bot.{EventHandler, MessageHandler}

trait BasicPlugin extends Plugin {

  def onMessage(bot: S2Bot): List[MessageHandler] = List.empty

  def onMention(bot: S2Bot): List[MessageHandler] = List.empty

  def onEvent(bot: S2Bot): List[EventHandler] = List.empty

  override def apply(bot: S2Bot): S2Bot = {
    val _1 = applies(bot)(onMessage)(_.hear)
    val _2 = applies(_1)(onMention)(_.respond)
    applies(_2)(onEvent)(_.onEvent)
  }

  private def applies[A](bot: S2Bot)(fn: S2Bot => List[A])(fn2: S2Bot => A => S2Bot): S2Bot = {
    fn(bot).foldLeft(bot)(fn2(_)(_))
  }
}
