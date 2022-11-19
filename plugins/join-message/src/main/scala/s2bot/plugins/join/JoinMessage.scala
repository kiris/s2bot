package s2bot.plugins.join

import akka.actor.ActorSystem
import s2bot.extensions.brain.Brain._
import s2bot.extensions.brain.{Brain, Codec}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.plugins.join.JoinMessage._
import s2bot.{Fmt, S2Bot, Plugin}
import slack.models.MemberJoined

import scala.concurrent.{ExecutionContext, Future}

class JoinMessage[A : Brain : DataCodec](brainKey: String = DEFAULT_BRAIN_KEY)(implicit system: ActorSystem) extends Plugin with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"join-message <message> - このチャンネルの、ジョインメッセージを設定します",
      s"join-message clear - このチャンネルの、ジョインメッセージを削除します",
      s"join-message - このチャンネルの、ジョインメッセージを表示します"
    )
  )

  override def apply(bot: S2Bot): S2Bot = {
    bot.hear {
      case ("Join-message", message) =>
        message.user.map { userId =>
          sayJoin(bot, userId, message.channel) {
            bot.reply(message,
              s"""${Fmt.linkChannel(message.channel)} にジョインメッセージは登録されてないよ。
                 |
                 |もしジョインメッセージを登録したい場合は `Join-message <message>` で登録してね。""".stripMargin)
          }
        }.getOrElse(Future.unit)

      case ("Join-message clear", message) =>
        for {
          _ <- clearJoinMessage(bot, message.channel)
          _ <- bot.say(message, "このチャンネルのジョインメッセージを削除したよ")
        } yield ()

      case (REGISTER_JOIN_PATTERN(joinMessage), message) =>
        for {
          _ <- registerJoinMessage(bot, message.channel, joinMessage)
          _ <- bot.say(message, "このチャンネルのジョインメッセージを設定したよ")
        } yield ()

    }.onEvent {
      case MemberJoined(userId, channelId, _) =>
        sayJoin(bot, userId, channelId)(Future.unit)
    }
  }

  private def registerJoinMessage(bot: S2Bot, channelId: String, joinMessage: String): Future[Unit] = {
    for {
      messagesOpt <- bot.brain[A].get(brainKey)
      _ <- {
        val oldJoinMessages = messagesOpt.getOrElse(Map.empty)
        val newJoinMessages = oldJoinMessages + (channelId -> joinMessage)
        bot.brain.set(brainKey, newJoinMessages)
      }
    } yield ()
  }

  private def clearJoinMessage(bot: S2Bot, channelId: String): Future[Unit] = {
    for {
      messagesOpt <- bot.brain[A].get(brainKey)
      _ <- {
        val oldJoinMessages = messagesOpt.getOrElse(Map.empty)
        val newJoinMessages = oldJoinMessages - channelId
        bot.brain.set(brainKey, newJoinMessages)
      }
    } yield ()
  }

  private def sayJoin(bot: S2Bot, userId: String, channelId: String)(or: => Future[AnyVal]): Future[AnyVal] = {
    for {
      messages <- bot.brain[A].get(brainKey)
      message <- {
        val message = for {
          messages <- messages
          message <- messages.get(channelId)
        } yield message

        message match {
          case Some(m) =>
            bot.say(channelId, s"${Fmt.linkUser(userId)} $m")
          case None =>
            or
        }
      }
    } yield message
  }
}

object JoinMessage {
  type Data = Map[String, String]

  type DataCodec[X] = Codec[Data, X]

  private val DEFAULT_BRAIN_KEY = "join-message"

  private val REGISTER_JOIN_PATTERN = "join-message ([\\s|\\S]+)".r
}