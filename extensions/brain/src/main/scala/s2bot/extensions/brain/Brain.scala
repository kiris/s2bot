package s2bot.extensions.brain

import s2bot.S2Bot

import scala.concurrent.Future

trait Brain {
  protected type E

  def get[A](key: String)(implicit codec: Codec[A, E]): Future[Option[A]]

  def set[A](key: String, value: A)(implicit codec: Codec[A, E]): Future[Boolean]
}

object Brain {
  implicit class S2BotOps(s2bot: S2Bot) {
    def brain[E](implicit brain: Brain): Brain = brain
  }
}

