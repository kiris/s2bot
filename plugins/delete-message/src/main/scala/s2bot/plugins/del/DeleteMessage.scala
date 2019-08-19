package s2bot.plugins.delete

import akka.actor.ActorSystem
import com.typesafe.config.Config
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.{S2Bot, Plugin}
import slack.models.{ReactionAdded, ReactionItemMessage}

class DeleteMessage(emojiName: String = "delete-action")(implicit system: ActorSystem) extends Plugin with Helpable {

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.REACTIONS -> List(
      s":$emojiName: - ${bot.me} のメッセージに :$emojiName: リアクションを付けと、そのメッセージを削除できます"
    )
  )

  override def apply(bot: S2Bot): S2Bot =
    bot.onEvent {
      case ReactionAdded(emoji, ReactionItemMessage(channel, ts), _, _, _) if emoji == emojiName =>
        bot.web.deleteChat(channel, ts)
    }

}

object DeleteMessage {
  def apply(config: Config)(implicit system: ActorSystem): DeleteMessage = new DeleteMessage(
    emojiName = config.getString("s2bot.plugins.deleteMessage.emojiName")
  )
}