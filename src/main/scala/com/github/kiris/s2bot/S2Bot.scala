package com.github.kiris.s2bot

import akka.actor.ActorSystem
import com.typesafe.config.Config
import slack.api.SlackApiClient
import slack.models.{Channel, Message, SlackEvent, User}
import slack.rtm.SlackRtmClient

import scala.concurrent.Future
import scala.concurrent.duration._


class S2Bot(val scripts: List[Script], token: String, config: Config, duration: FiniteDuration = 5.seconds) {
  implicit private val system = ActorSystem("slack", config)

  implicit private val ec = system.dispatcher

  private val errorHandlers: collection.mutable.ListBuffer[PartialFunction[Throwable, Unit]] =
    collection.mutable.ListBuffer[PartialFunction[Throwable, Unit]]()

  private val sendMessageHooks: collection.mutable.ListBuffer[(String, String) => (String, String)] =
    collection.mutable.ListBuffer[(String, String) => (String, String)]()

  val rtm = SlackRtmClient(token, duration)

  val web = SlackApiClient(token)

  def state = rtm.state

  def self = state.self

  val me = Fmt.linkUser(self.id)

  def run(): Unit = scripts.foreach(_.apply(this))

  def hear(pf: PartialFunction[(String, Message), Future[Any]]): Unit =
    rtm.onMessage { message =>
      exec {
        pf.lift((message.text.trim, message))
      }
    }

  def respond(pf: PartialFunction[(String, Message), Future[Any]]): Unit =
    rtm.onMessage { message =>
      exec {
         if (message.text.startsWith(me)) {
          pf.lift((message.text.substring(me.length).trim, message))
        } else {
          None
        }
      }
    }

  def onEvent(pf: PartialFunction[SlackEvent, Future[Any]]): Unit =
    rtm.onEvent { event =>
      exec {
        pf.lift(event)
      }
    }

  def onError(pf: PartialFunction[Throwable, Unit]): Unit = errorHandlers += pf

  def addSendMessageHook(hook: (String, String) => (String, String)): Unit = sendMessageHooks += hook

  def exec[T](f: => Option[Future[T]]): Unit =
    f match {
      case Some(result) => result.failed.foreach { ex =>
        val results = errorHandlers.flatMap(_.lift(ex))
        if (results.isEmpty) {
          ex.printStackTrace()
        }
      }
      case _ => ()
    }


  def say(channelId: String, text: String): Future[Long] = {
    val (c, t) = sendMessageHooks.foldLeft(channelId, text) { case ((channelId, text), hook) =>
      hook(channelId, text)
    }

    rtm.sendMessage(c, t)
  }

  def say(channel: Channel, text: String): Future[Long] = say(channel.id, text)

  def say(message: Message, text: String): Future[Long] = say(message.channel, text)

  def reply(message: Message, text: String): Future[Long] = say(message, s"${Fmt.linkUser(message.user)} $text")

  def reaction(channelId: String, timestamp: String, emojiName: String): Future[Boolean] =
    web.addReaction(emojiName, channelId = Some(channelId), timestamp = Some(timestamp))

  def reaction(message: Message, emojiName: String): Future[Boolean] = reaction(message.channel, message.ts, emojiName)

  def getChannelIdForName(name: String): Option[String] = state.getChannelIdForName(name)

  def getUserIdForName(name: String): Option[String] = state.getUserIdForName(name)

  def getUser(id: String): Option[User] = state.getUserById(id)

  def getChannel(id: String): Option[Channel] = state.channels.find(_.id == id)
}
