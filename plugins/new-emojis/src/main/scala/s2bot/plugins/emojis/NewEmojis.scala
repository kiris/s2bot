package s2bot.plugins.emojis

import akka.actor.ActorSystem
import s2bot.extensions.brain.Brain._
import s2bot.extensions.brain.Brain
import s2bot.extensions.cron.CronJob._
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.{Fmt, S2Bot, Script}

import scala.concurrent.{ExecutionContext, Future}

class NewEmojis[A : Brain : Brain[A]#CODEC](channelName: String)(implicit system: ActorSystem) extends Script with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.CHANNELS -> List(
      s"${Fmt.linkChannelForName(bot, channelName)} - 新しい絵文字が追加されたら通知します"
    )
  )

  override def apply(bot: S2Bot): Unit = {
    bot.job("Every minute") {
      notifyNewEmojis(bot)
    }
  }

  private def notifyNewEmojis(bot: S2Bot): Future[Unit] = {
    for {
      emojis <- bot.web.listEmojis().map(_.keySet)
      oldEmojis <- bot.brain[A].get[Set[String]]("emojis").map(_.getOrElse(Set.empty))
      result <- emojis -- oldEmojis match {
        case diff if diff.isEmpty => Future.unit
        case diff =>
          val newEmojis = diff.take(10).map { name => s":$name:(`:$name:`)" }.mkString(" ")
          for {
            _ <- if (oldEmojis.isEmpty) Future.unit else sayNewEmojis(bot, newEmojis)
            _ <- bot.brain.set("emojis", emojis)
          } yield ()
      }
    } yield result
  }

  private def sayNewEmojis(bot: S2Bot, newEmojis: String): Future[Any] =
    bot.getChannelIdForName(channelName) match {
      case Some(channelId) => bot.say(channelId, s"新しい絵文字 $newEmojis が追加されたよ")
      case None => Future.unit
    }
}

