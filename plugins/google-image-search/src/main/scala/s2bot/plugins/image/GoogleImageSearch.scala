package s2bot.plugins.image

import com.typesafe.config.Config
import s2bot.{S2Bot, Script}
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import slack.models.Message

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class GoogleImageSearch(cseId: String, apiKey: String)(implicit executionContext: ExecutionContext) extends Script with Helpable {

  private val customSearchClient = new CustomSearchClient(cseId, apiKey)

  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      "image <query> - 画像を検索して表示します",
      "anime <query> -  GIFアニメ画像を検索して表示します"
    )
  )

  private val IMAGE_PATTERN = "image (.+)".r

  private val ANIME_PATTERN = "anime (.+)".r

  override def apply(bot: S2Bot): Unit = {
    bot.hear {
      case (IMAGE_PATTERN(q), message) =>
        imageSearch(bot, message, q)

      case (ANIME_PATTERN(q), message) =>
        imageSearch(bot, message, q, true)
    }
  }

  private[this] def imageSearch(bot: S2Bot, message: Message, q: String, animate: Boolean = false): Future[Unit] =
    for {
      images <- customSearchClient.searchImage(q, animate = animate)
      _ <- Random.shuffle(images).headOption match {
        case None => bot.say(message, s"画像がみつからなかったよ")
        case Some(image) => bot.say(message, ensureResult(image.link, animate))
      }
    } yield ()


  private[this] def ensureResult(url: String, animate: Boolean) =
    ensureImageExtension(
      if (animate) {
        url.replaceAll("(giphy\\.com/.*)/.+_s.gif$", "$1/giphy.gif")
      } else {
        url
      }
    )

  private[this] def ensureImageExtension(url: String) =
    if (url.matches(".+(png|jpe?g|gif)$")) {
      url
    } else {
      s"${url}#.png"
    }
}


object GoogleImageSearch {
  def apply(config: Config)(implicit executionContext: ExecutionContext): GoogleImageSearch = new GoogleImageSearch(
    cseId = config.getString("s2bot.plugins.googleImageSearch.cseId"),
    apiKey = config.getString("s2bot.plugins.googleImageSearch.apiKey"),
  )
}