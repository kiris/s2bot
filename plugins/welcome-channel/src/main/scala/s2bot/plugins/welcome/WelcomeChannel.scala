package s2bot.plugins.welcome

import akka.actor.ActorSystem
import s2bot.extensions.brain.Brain._
import s2bot.extensions.brain.{Brain, Codec}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.plugins.welcome.WelcomeChannel._
import s2bot.{Fmt, S2Bot, Script}
import slack.models.MemberJoined

import scala.concurrent.{ExecutionContext, Future}

class WelcomeChannel[A : Brain : DataCodec](brainKey: String = DEFAULT_BRAIN_KEY)(implicit system: ActorSystem) extends Script with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"welcome <message> - このチャンネルに新しいユーザーがジョインしたウェルカムメッセージを表示します",
      s"welcome - 現在このチャンネルに設定されている、ウェルカムメッセージを表示します"
    )
  )

  override def apply(bot: S2Bot): Unit = {
    bot.hear {
      case ("welcome", message) =>
        sayWelcome(bot, message.user, message.channel) {
          bot.reply(message,
            s"""${Fmt.linkChannel(message.channel)} にウェルカムメッセージは登録されてないよ。
               |
               |もしウェルカムメッセージを登録したい場合は `welcome <message>` で登録してね。""".stripMargin)
        }

      case (REGISTER_WELCOME_PATTERN(welcomeMessage), message) =>
        for {
          _ <- registerWelcomeMessage(bot, message.channel, welcomeMessage)
          _ <- bot.say(message, "このチャンネルのウェルカムメッセージを設定したよ")
        } yield ()
    }

    bot.onEvent {
      case MemberJoined(userId, channelId, _) =>
        sayWelcome(bot, userId, channelId)(Future.unit)
    }
  }

  private def registerWelcomeMessage(bot: S2Bot, channelId: String, welcomeMessage: String): Future[Unit] = {
    for {
      messagesOpt <- bot.brain[A].get(brainKey)
      _ <- {
        val oldWelcomeMessages = messagesOpt.getOrElse(Map.empty)
        val newWelcomeMessages = oldWelcomeMessages + (channelId -> welcomeMessage)
        bot.brain.set(brainKey, newWelcomeMessages)
      }
    } yield ()
  }

  private def sayWelcome(bot: S2Bot, userId: String, channelId: String)(or: => Future[AnyVal]): Future[AnyVal] = {
    for {
      messages <- bot.brain[A].get(brainKey)
      message <- {
        val message = for {
          messages <- messages
          message <- messages.get(channelId)
        } yield message

        message match {
          case Some(m) =>
            bot.say(channelId, m)
          case None =>
            or
        }
      }
    } yield message
  }
}

object WelcomeChannel {
  type Data = Map[String, String]

  type DataCodec[X] = Codec[Data, X]

  val DEFAULT_BRAIN_KEY = "welcome-channel"

  val REGISTER_WELCOME_PATTERN = "welcome ([\\s|\\S]+)".r
}