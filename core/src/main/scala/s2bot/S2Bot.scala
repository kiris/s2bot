package s2bot

import akka.actor.ActorSystem
import com.typesafe.config.Config
import s2bot.S2Bot._
import slack.api.SlackApiClient
import slack.models.{Channel, Message, SlackEvent, User}
import slack.rtm.{RtmState, SlackRtmClient}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class SlackClient(
    val rtm: SlackRtmClient,
    val web: SlackApiClient,
    val actorSystem: ActorSystem
)

object SlackClient {
  def factory(
      token: String,
      duration: FiniteDuration = 5.seconds,
      config: Config
  ): SlackClient = {
    implicit val system: ActorSystem = ActorSystem("slack", config)

    new SlackClient(
      rtm = SlackRtmClient(token = token, duration = duration),
      web = SlackApiClient(token),
      actorSystem = system
    )
  }
}

case class S2Bot(
    private val client: SlackClient,
    config: Config,
    plugins: List[Plugin] = Nil,
    hearHandlers: List[MessageHandler] = Nil,
    responseHandlers: List[MessageHandler] = Nil,
    eventHandlers: List[EventHandler] = Nil,
    errorHandlers: List[ErrorHandler] = Nil,
    scripts: List[Script] = Nil,
    sendMessageHooks: List[SendMessageHook] = Nil,
) {

  implicit private def actorSystem = client.actorSystem

  implicit private def ec = client.actorSystem.dispatcher

  def rtm: SlackRtmClient = client.rtm

  def web: SlackApiClient = client.web

  def rtmState: RtmState = rtm.state

  def self: User = rtmState.self

  def me: String = Fmt.linkUser(self.id)

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

  def reply(message: Message, text: String): Future[Long] = message.user match {
    case Some(u) => say(message, s"${Fmt.linkUser(u)} $text")
    case None => Future(0L)
  }

  private def sendMessage(sendMssage: SendMessage): Future[Long] = {
    val sm = sendMessageHooks.foldLeft(sendMssage) { case (sm, hook) =>
      hook(sm)
    }
    rtm.sendMessage(sm.channelId, sm.text, sm.threadId)
  }

  def reaction(channelId: String, timestamp: String, emojiName: String): Future[Boolean] =
    web.addReaction(emojiName, channelId = Some(channelId), timestamp = Some(timestamp))

  def reaction(message: Message, emojiName: String): Future[Boolean] = reaction(message.channel, message.ts, emojiName)

  def getChannelIdForName(name: String): Future[Option[String]] =
    web.listConversations().map(_.collectFirst { case c if c.name == name => c.id })

  def getUserIdForName(name: String): Future[Option[String]] =
    web.listUsers().map(_.collectFirst { case u if u.name == name => u.id })

  def getUser(id: String): Future[User] = web.getUserInfo(id)

  def getChannel(id: String): Future[Channel] = web.getConversationInfo(id)

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