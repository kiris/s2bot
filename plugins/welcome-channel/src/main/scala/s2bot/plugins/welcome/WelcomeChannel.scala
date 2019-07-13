package s2bot.plugins.welcome

import com.typesafe.config.Config
import s2bot.extensions.brain.{Brain, Codec}
import s2bot.{Fmt, S2Bot, Script}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import slack.models.{MemberJoined, Message}

import scala.concurrent.Future
import WelcomeChannel._
import Brain._

class WelcomeChannel[A : Brain : C](brainKey: String = "welcome-channel") extends Script with Helpable {

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"welcome <message> - このチャンネルに新しいユーザーがジョインしたら<message>を表示します"
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
      messagesOpt <- bot.brain.get(brainKey)[Data]
      _ <- {
        val oldWelcomeMessages = messagesOpt.getOrElse(Map.empty)
        val newWelcomeMessages = oldWelcomeMessages + (channelId -> welcomeMessage)
        bot.brain.set(brainKey, newWelcomeMessages)
      }
    } yield ()
  }

  private def sayWelcome(bot: S2Bot, userId: String, channelId: String)(or: => Future[AnyVal]): Future[AnyVal] = {
    for {
      messages <- bot.brain.get[Data](brainKey)
      message <- {
        val message = for {
          messages <- messages
          message <- messages.get(channelId)
        } yield message

        message match {
          case Some(m) =>
            bot.say(channelId,
              s"""${Fmt.linkUser(userId)} ${Fmt.linkChannel(channelId)} にようこそ！
                 |
                 |$m""".stripMargin)
          case None =>
            or()
        }
      }
    } yield message
  }
}

object WelcomeChannel {
  type Data = Map[String, String]
  type C[X] = Codec[Data, X]

  val REGISTER_WELCOME_PATTERN = "welcome ([\\s|\\S]+)".r
}