package s2bot.extensions.brain

import s2bot.S2Bot

import scala.concurrent.Future

trait Brain[E] {
  def get[A](key: String)(implicit codec: Codec[A, E]): Future[Option[A]]

  def set[A](key: String, value: A)(implicit codec: Codec[A, E]): Future[Boolean]
}

object Brain {
  implicit class BrainOps(val s2bot: S2Bot) extends AnyVal {
    def brain[E : Brain]: Brain[E] = implicitly[Brain[E]]
  }
}

