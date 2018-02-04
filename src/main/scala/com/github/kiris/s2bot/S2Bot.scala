package com.github.kiris.s2bot

import akka.actor.ActorSystem
import com.typesafe.config.Config
import slack.models.{Message, SlackEvent}
import slack.rtm.SlackRtmClient

import scala.concurrent.Future
import scala.util.{Success, Try}

class S2Bot(val scripts: List[Script], token: String, config: Config) {
  implicit private val system = ActorSystem("slack", config)

  implicit private val ec = system.dispatcher

  val client = SlackRtmClient(token)

  val selfId = client.state.self.id

  val me = s"<@${selfId}>"

  val errorHandlers: collection.mutable.ListBuffer[PartialFunction[Throwable, Unit]] = collection.mutable.ListBuffer[PartialFunction[Throwable, Unit]]()

  def run(): Unit = scripts.foreach(_.apply(this))

  def hear(pf: PartialFunction[(String, Message), Unit]): Unit =
    client.onMessage { message =>
      dispatch(pf, (message.text.trim, message))
    }

  def respond(pf: PartialFunction[(String, Message), Unit]): Unit =
    client.onMessage { message =>
      if (message.text.startsWith(me)) {
        dispatch(pf, (message.text.trim, message))
      }
    }

  def onEvent(pf: PartialFunction[SlackEvent, Unit]): Unit =
    client.onEvent { event =>
      dispatch(pf, event)
    }

  def onError(pf: PartialFunction[Throwable, Unit]): Unit = {
    errorHandlers += pf
  }

  private def dispatch[T](pf: PartialFunction[T, Unit], msg: T) = {
    Try {
      pf.lift(msg)
    } recover {
      case x: Throwable =>
        val results = errorHandlers.map { h =>
          h.lift(x)
        }

        if (results.forall(_.isEmpty)) {
          x.printStackTrace()
        }
    }
  }

  def say(channelId: String, text: String): Future[Long] = client.sendMessage(channelId, text)

  def say(message: Message, text: String): Future[Long] = say(message.channel, text)

  def reply(message: Message, text: String): Future[Long] = say(message, s"<@${message.user}> $text")


}

