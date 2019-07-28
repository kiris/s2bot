package s2bot.plugins.userdict


import akka.actor.ActorSystem
import s2bot.extensions.brain.Brain._
import s2bot.extensions.brain.{Brain, Codec}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.plugins.userdict.UserDictionary._
import s2bot.{S2Bot, Script}

import scala.concurrent.{ExecutionContext, Future}

class UserDictionary[A : Brain : DataCodec](brainKey: String = DEFAULT_BRAIN_KEY)(implicit system: ActorSystem) extends Script with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      "userdict list - ユーザー辞書に登録されているキーワードの一覧を返します",
      "userdict <keyword> - ユーザー辞書の<keyword>に登録されたメッセージを返します",
      "userdict <keyword> <message> - ユーザー辞書の<keyword>に<message>を登録します",
      "userdict <keyword> del - ユーザー辞書の<keyword>を削除します"
    )
  )


  override def apply(bot: S2Bot): Unit = {
    bot.hear {
      case (LIST_KEYWORDS_PATTERN, message) =>
        for {
          keys <- listKeywords(bot, message.user)
          _ <- keys match {
            case Nil => bot.say(message, "登録しているメッセージは無いよ")
            case ks => bot.say(message, ks.map("- " + _)mkString("\n"))
          }
        } yield ()

      case (SHOW_RESPONSE_PATTERN(key), message) =>
        for {
          responseOpt <- getMyResponse(bot, key, message.user)
          _ <- responseOpt match {
            case Some(response) => bot.say(message, response)
            case None => bot.say(message, s"${key}に登録しているメッセージは無いよ")
          }
        } yield ()

      case (UNREGISTER_RESPONSE_PATTERN(key), message) =>
        for {
          _ <- unregisterMyResponse(bot, message.user, key)
          _ <- bot.say(message, s"${key}のメッセージを削除したよ")
        } yield ()

      case (REGISTER_RESPONSE_PATTERN(key, response), message) =>
        for {
          _ <- registerMyResponse(bot, message.user, key, response)
          _ <- bot.say(message, s"${key}にメッセージを登録したよ")
        } yield ()
    }
  }

  private def listKeywords(bot: S2Bot, userId: UserId): Future[List[String]] = {
    for {
      dataOpt <- bot.brain[A].get(brainKey)
    } yield {
      for {
        data <- dataOpt.toList
        userData <- data.get(userId).toList
        keyword <- userData.keys
      } yield keyword
    }
  }

  private def getMyResponse(bot: S2Bot, userId: UserId, keyword: String): Future[Option[String]] = {
    for {
      dataOpt <- bot.brain[A].get(brainKey)
    } yield {
      for {
        data <- dataOpt
        userData <- data.get(userId)
        userResponse <- userData.get(keyword)
      } yield userResponse
    }
  }

  private def unregisterMyResponse(bot: S2Bot, userId: UserId, keyword: String): Future[Unit] = {
    for {
      dataOpt <- bot.brain[A].get(brainKey)
      _ <- {
        val oldData = dataOpt.getOrElse(Map.empty)
        val oldUserData = oldData.getOrElse(userId, Map.empty)
        val newUserData = oldUserData - keyword
        val newData = oldData + (userId -> newUserData)
        bot.brain.set(brainKey, newData)
      }
    } yield ()
  }

  private def registerMyResponse(bot: S2Bot, userId: UserId, keyword: String, response: String): Future[Unit] = {
    for {
      dataOpt <- bot.brain[A].get(brainKey)
      _ <- {
        val oldData = dataOpt.getOrElse(Map.empty)
        val oldUserData = oldData.getOrElse(userId, Map.empty)
        val newUserData = oldUserData + (keyword -> response)
        val newData = oldData + (userId -> newUserData)
        bot.brain.set(brainKey, newData)
      }
    } yield ()
  }
}

private object UserDictionary {
  val DEFAULT_BRAIN_KEY = "userdict"

  val LIST_KEYWORDS_PATTERN = "userdict list"

  val SHOW_RESPONSE_PATTERN = "userdict ([^ ]+)".r

  val REGISTER_RESPONSE_PATTERN = "userdict ([^ ]+) ([\\s|\\S]+)".r

  val UNREGISTER_RESPONSE_PATTERN = "userdict ([^ ]+) del".r

  type UserId = String

  type Data = Map[UserId, Map[String, String]]

  type DataCodec[X] = Codec[Data, X]
}