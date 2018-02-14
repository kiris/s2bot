package com.github.kiris.s2bot

import java.net.URI

import akka.actor.ActorSystem
import com.typesafe.config.Config
import slack.api.BlockingSlackApiClient
import slack.models.{Channel, Message, SlackEvent, User}
import slack.rtm.SlackRtmClient

import scala.concurrent.Future
import scala.util.Try

class S2Bot(val scripts: List[Script], token: String, config: Config) {
  implicit private val system = ActorSystem("slack", config)

  implicit private val ec = system.dispatcher

  private val errorHandlers: collection.mutable.ListBuffer[PartialFunction[Throwable, Unit]] = collection.mutable.ListBuffer[PartialFunction[Throwable, Unit]]()

  val rtm = SlackRtmClient(token)

  val web = BlockingSlackApiClient(token)

  def state = rtm.state

  val me = s"<@${state.self.id}>"

  def run(): Unit = scripts.foreach(_.apply(this))

  def hear(pf: PartialFunction[(String, Message), Unit]): Unit =
    rtm.onMessage { message =>
      exec {
        pf.lift((message.text.trim, message))
      }
    }

  def respond(pf: PartialFunction[(String, Message), Unit]): Unit =
    rtm.onMessage { message =>
      exec {
        if (message.text.startsWith(me)) {
          pf.lift((message.text.substring(me.length).trim, message))
        }
      }
    }

  def onEvent(pf: PartialFunction[SlackEvent, Unit]): Unit =
    rtm.onEvent { event =>
      exec {
        pf.lift(event)
      }
    }

  def onError(pf: PartialFunction[Throwable, Unit]): Unit = errorHandlers += pf

  def exec[T](f: => T): Unit = {
    Try {
      f
    } recover {
      case x: Throwable =>
        val results = errorHandlers.flatMap(_.lift(x))
        if (results.isEmpty) {
          x.printStackTrace()
        }
    }
  }

  def say(channelId: String, text: String): Future[Long] = rtm.sendMessage(channelId, text)

  def say(channel: Channel, text: String): Future[Long] = say(channel.id, text)

  def say(message: Message, text: String): Future[Long] = say(message.channel, text)

  def reply(message: Message, text: String): Future[Long] = say(message, s"<@${message.user}> $text")

  def getChannelIdForName(name: String): Option[String] = state.getChannelIdForName(name)

  def getUserIdForName(name: String): Option[String] = state.getUserIdForName(name)

  def getUser(id: String): Option[User] = state.getUserById(id)

  def getChannel(id: String): Option[Channel] = state.channels.find(_.id == id)

  def toLinkUrl(channelId: String, ts: String): URI = new URI(s"https://${state.team.domain}.slack.com/archives/$channelId/p${ts.replaceAll("\\.", "")}")
}

