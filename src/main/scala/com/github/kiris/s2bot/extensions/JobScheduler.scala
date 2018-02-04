package com.github.kiris.s2bot.extensions

import akka.actor.{Actor, ActorSystem, Props}
import com.github.kiris.s2bot.S2Bot
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

import scala.util.Try


class JobScheduler(actorSystem: ActorSystem) {

  private val scheduler = QuartzSchedulerExtension(actorSystem)

  def job(scheduleKey: String, job: => Try[Any]) = {
    actorSystem.settings.config.getConfig("")
    val props = actorSystem.actorOf(Props(classOf[JobActor], job))
    scheduler.schedule(scheduleKey, props, Unit)
  }
}

private class JobActor(job: => Try[Any]) extends Actor {
  def receive = {
    case _ =>
      job
  }
}

