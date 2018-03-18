package com.github.kiris.s2bot.extensions


import com.github.kiris.s2bot.S2Bot
import cronish._
import cronish.dsl._

import scala.concurrent.Future

object CronJob {
  private def job[T](cron: Cron)(cmd: => T): Scheduled = task(cmd) executes (cron)

  object Implicits {
    implicit class S2BotSyntax(val s2bot: S2Bot) extends AnyVal {
      def job[T](cronString: String)(cmd: => Future[T]): Scheduled = this.job(cronString.cron)(cmd)

      def job[T](cron: Cron)(cmd: => Future[T]): Scheduled =
        CronJob.job(cron) {
          s2bot.exec {
            Some(cmd)
          }
        }
    }
  }
}