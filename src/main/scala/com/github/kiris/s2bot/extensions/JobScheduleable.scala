package com.github.kiris.s2bot.extensions

import akka.actor.{Actor, ActorSystem, Props}
import com.github.kiris.s2bot.S2Bot
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

import scala.util.Try


object JobScheduleable {

  implicit class JobScheduleable(val s2Bot: S2Bot) extends AnyVal {
    def job(scheduleKey: String, job: => Try[Any])(implicit actorSystem: ActorSystem) = {
      val scheduler = QuartzSchedulerExtension.get(actorSystem)
      val props = actorSystem.actorOf(Props(classOf[JobActor], job))
      scheduler.schedule(scheduleKey, props, Unit)
    }
  }

  private class JobActor(job: => Try[Any]) extends Actor {
    def receive = {
      case _ =>
        job()
    }
  }

}
