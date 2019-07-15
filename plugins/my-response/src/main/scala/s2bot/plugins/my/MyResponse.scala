package s2bot.plugins.my


import akka.actor.ActorSystem
import s2bot.extensions.brain.Brain._
import s2bot.extensions.brain.{Brain, Codec}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.plugins.my.MyResponse._
import s2bot.{S2Bot, Script}

import scala.concurrent.{ExecutionContext, Future}

class MyResponse[B : Brain : C](brainKey: String = "my-response")(implicit system: ActorSystem) extends Script with Helpable {
    implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      "my list - メッセージを登録したキーワードの一覧を返します",
      "my <keyword> - <keyword>に登録したメッセージを返します",
      "my <keyword> <message> - <keyword>にメッセージを登録します",
      "my <keyword> del - <keyword>に登録したメッセージを削除します"
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

  private def listKeywords(bot: S2Bot, userId: String): Future[List[String]] = {
    for {
      dataOpt <- bot.brain.get(brainKey)[Data]
    } yield {
      for {
        data <- dataOpt.toList
        userData <- data.get(userId)
        keyword <- userData.keys
      } yield keyword
    }
  }

  private def getMyResponse(bot: S2Bot, userId: String, keyword: String): Future[Option[String]] = {
    for {
      dataOpt <- bot.brain.get(brainKey)[Data]
    } yield {
      for {
        data <- dataOpt
        userData <- data.get(userId)
        userResponse <- userData.get(keyword)
      } yield userResponse
    }
  }

  private def unregisterMyResponse(bot: S2Bot, userId: String, keyword: String): Future[Unit] = {
    for {
      dataOpt <- bot.brain.get(brainKey)[Data]
      _ <- {
        val oldData = dataOpt.getOrElse(Map.empty)
        val oldUserData = oldData.getOrElse(userId, Map.empty)
        val newUserData = oldUserData - keyword
        val newData = oldData + (userId -> newUserData)
        bot.brain.set(brainKey, newData)
      }
    } yield ()
  }

  private def registerMyResponse(bot: S2Bot, userId: String, keyword: String, response: String): Future[Unit] = {
    for {
      dataOpt <- bot.brain.get(brainKey)[Data]
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

private object MyResponse {
  val BRAIN_KEY = "my-response"

  val LIST_KEYWORDS_PATTERN = "my list"

  val SHOW_RESPONSE_PATTERN = "my ([^ ]+)".r

  val REGISTER_RESPONSE_PATTERN = "my ([^ ]+) ([\\s|\\S]+)".r

  val UNREGISTER_RESPONSE_PATTERN = "my ([^ ]+) del".r

  type Data = Map[String, Map[String, String]]

  type C[X] = Codec[Data, X]
}