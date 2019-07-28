package s2bot.extensions.cron

import cronish.Cron
import cronish.dsl._
import s2bot.S2Bot

import scala.concurrent.Future

object CronJob {
  private def job[T](cron: Cron)(cmd: => T): Scheduled = task(cmd) executes cron

  implicit class CronJobOps(val s2bot: S2Bot) extends AnyVal {
    def job[T](cronString: String)(cmd: => Future[T]): Scheduled = this.job(cronString.cron)(cmd)

    def job[T](cron: Cron)(cmd: => Future[T]): Scheduled =
      CronJob.job(cron) {
        s2bot.exec {
          Some(cmd)
        }
      }
  }
}
