package com.github.kiris.s2bot.extensions


import com.github.kiris.s2bot.S2Bot
import cronish._
import cronish.dsl._

object CronJob {
  def job[T](cronString: String, job: => T): Scheduled = this.job(cronString.cron, job)

  def job[T](cron: Cron, job: => T): Scheduled = task(job) executes (cron)

  object Implicits {
    implicit class S2BotSyntax(val s2bot: S2Bot) extends AnyVal {
      def job[T](cronString: String, job: => T): Scheduled = CronJob.job(cronString, job)

      def job[T](cron: Cron, job: => T): Scheduled = CronJob.job(cron, job)
    }
  }
}