package s2bot.plugins.delete

import akka.actor.ActorSystem
import com.typesafe.config.Config
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.{S2Bot, Script}
import slack.models.{ReactionAdded, ReactionItemMessage}

class DeleteMessage(emojiName: String = "delete-action")(implicit system: ActorSystem) extends Script with Helpable {

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.REACTIONS -> List(
      s":$emojiName: - :$emojiName のリアクションが付けられたメッセージを削除します"
    )
  )

  override def apply(bot: S2Bot): Unit =
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