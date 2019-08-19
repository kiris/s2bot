package s2bot

import akka.actor.ActorSystem
import com.typesafe.config.Config
import s2bot.S2Bot._
import slack.api.SlackApiClient
import slack.models.{Channel, Message, SlackEvent, User}
import slack.rtm.{RtmState, SlackRtmClient}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

case class S2Bot(
    token: String,
    config: Config,
    plugins: List[Plugin] = Nil,
    hearHandlers: List[MessageHandler] = Nil,
    responseHandlers: List[MessageHandler] = Nil,
    eventHandlers: List[EventHandler] = Nil,
    errorHandlers: List[ErrorHandler] = Nil,
    scripts: List[Script] = Nil,
    sendMessageHooks: List[SendMessageHook] = Nil,
    duration: FiniteDuration = 5.seconds
) {
  implicit private val system = ActorSystem("slack", config)

  implicit private val ec = system.dispatcher

  val rtm: SlackRtmClient = SlackRtmClient(token = token, duration = duration)

  val web: SlackApiClient = SlackApiClient(token)

  def state: RtmState = rtm.state

  def self: User = state.self

  val me: String = Fmt.linkUser(self.id)

  def addPlugins(plugins: Plugin*): S2Bot = this.copy(plugins = this.plugins ++ plugins)

  def hear(handler: MessageHandler): S2Bot = this.copy(hearHandlers = this.hearHandlers :+ handler)

  def respond(handler: MessageHandler): S2Bot = this.copy(responseHandlers = this.responseHandlers :+ handler)

  def onEvent(handler: EventHandler): S2Bot = this.copy(eventHandlers = this.eventHandlers :+ handler)

  def onError(handler: ErrorHandler): S2Bot = this.copy(errorHandlers = this.errorHandlers :+ handler)

  def addScript(script: Script): S2Bot = this.copy(scripts = this.scripts :+ script)

  def addSendMessageHook(hook: SendMessageHook): S2Bot = this.copy(sendMessageHooks = this.sendMessageHooks :+ hook)

  def say(channelId: String, text: String, threadId: Option[String] = None): Future[Long] = {
    sendMessage(SendMessage(channelId, text, threadId))
  }

  def say(message: Message, text: String): Future[Long] = say(message.channel, text, message.thread_ts)

  def reply(message: Message, text: String): Future[Long] = say(message, s"${Fmt.linkUser(message.user)} $text")

  private def sendMessage(sendMssage: SendMessage): Future[Long] = {
    val sm = sendMessageHooks.foldLeft(sendMssage) { case (sm, hook) =>
      hook(sm)
    }
    rtm.sendMessage(sm.channelId, sm.text, sm.threadId)
  }

  def reaction(channelId: String, timestamp: String, emojiName: String): Future[Boolean] =
    web.addReaction(emojiName, channelId = Some(channelId), timestamp = Some(timestamp))

  def reaction(message: Message, emojiName: String): Future[Boolean] = reaction(message.channel, message.ts, emojiName)

  def getChannelIdForName(name: String): Option[String] = state.getChannelIdForName(name)

  def getUserIdForName(name: String): Option[String] = state.getUserIdForName(name)

  def getUser(id: String): Option[User] = state.getUserById(id)

  def getChannel(id: String): Option[Channel] = state.channels.find(_.id == id)

  private def applyPlugins(): S2Bot = {
    plugins.foldLeft(this) { (s2bot, plugin) =>
      plugin.apply(s2bot)
    }
  }

  private def registerRtmHandlers(): Unit = {
    hearHandlers.foreach { handler =>
      rtm.onMessage { message =>
        for {
          result <- handler.lift((message.text.trim, message))
        } yield recoverErrors(result)
      }
    }

    responseHandlers.foreach { handler =>
      rtm.onMessage { message =>
        for {
          result <- {
            if (message.text.startsWith(me)) {
              handler.lift((message.text.substring(me.length).trim, message))
            } else {
              None
            }
          }
        } yield recoverErrors(result)
      }
    }

    eventHandlers.foreach { handler =>
      rtm.onEvent { event =>
        for {
          result <- handler.lift(event)
        } yield recoverErrors(result)
      }
    }
  }

  private def evalScripts(): Unit = {
    val future = Future.traverse(scripts) { script =>
      script(this)
    }

    Await.result(future, Duration.Inf)

    ()
  }


  def recoverErrors(f: Future[Any]): Unit = {
    for {
      ex <- errorHandlers.foldLeft(f) { (result, handler) =>
        result.recoverWith(handler)
      }.failed
    } yield ex.printStackTrace()
  }

  def run(): Unit = {
    val bot = applyPlugins()
    bot.registerRtmHandlers()
    bot.evalScripts()
  }
}

object S2Bot {

  type HandleMessageHook = (Message, MessageHandler) => Future[Any]

  type MessageHandler = PartialFunction[(String, Message), Future[Any]]

  type EventHandler = PartialFunction[SlackEvent, Future[Any]]

  type ErrorHandler = PartialFunction[Throwable, Future[Any]]

  case class SendMessage(channelId: String, text: String, threadId: Option[String])

  type SendMessageHook = SendMessage => SendMessage
}