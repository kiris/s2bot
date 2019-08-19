package s2bot.extensions.cron

import cronish.Cron
import cronish.dsl._
import s2bot.{S2Bot, Script}

import scala.util.Try
import scala.concurrent.Future


object CronJob {
  implicit class CronJobOps(val s2bot: S2Bot) extends AnyVal {
    def cronJob[T](cronString: String)(cmd: => Future[T]): S2Bot = this.cronJob(cronString.cron)(cmd)

    def cronJob[T](cron: Cron)(cmd: => Future[T]): S2Bot =
      s2bot.addScript {
        new Script {
          override def apply(bot: S2Bot): Future[Any] = Future.fromTry {
            Try {
              task {
                s2bot.recoverErrors(cmd)
              } executes cron
            }
          }
        }
      }

  }
}
