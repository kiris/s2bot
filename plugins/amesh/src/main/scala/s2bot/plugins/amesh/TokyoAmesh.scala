package s2bot.plugins.amesh

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import akka.actor.ActorSystem
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.composite.AlphaComposite
import dispatch._
import s2bot.buildin.Helpable
import s2bot.buildin.Helpable.{DefaultKeys, Usage}
import s2bot.{S2Bot, Script}

import scala.concurrent.{ExecutionContext, Future}

class TokyoAmesh(implicit system: ActorSystem) extends Script with Helpable {

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Usage = Usage(
    DefaultKeys.COMMANDS -> List(
      s"amesh - 東京アメッシュの雨雲レーダーを表示します"
    )
  )

  override def apply(bot: S2Bot): Unit = {
    bot.hear {
      case ("amesh", msg) =>
        val now = LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
        for {
          ameshImage <- amesh(now)
          _ <- bot.web.uploadFile(
            content = Right(ameshImage.bytes),
            filename = Some(s"${"%tY/%<tm/%<td %<tH:%<tM".format(now)}.png"),
            channels = Some(Seq(msg.channel))
          )
        } yield ()
    }
  }

  private def getImage(u: String): Future[Image] = {
    val svc = url(u)
    Http.withConfiguration(_ setFollowRedirect true)(svc OK as.Bytes).map(Image(_))
  }

  private[amesh] def amesh(dateTime: LocalDateTime): Future[Image] = {
    val n = dateTime.minus((dateTime.getMinute % 5) + 5, ChronoUnit.MINUTES)
    val str = "%tY%<tm%<td%<tH%<tM".format(n)
    for {
      imageMsk <- getImage("http://tokyo-ame.jwa.or.jp/map/msk000.png")
      imageMap <- getImage("http://tokyo-ame.jwa.or.jp/map/map000.jpg")
      imageWeather <- getImage(s"http://tokyo-ame.jwa.or.jp/mesh/000/$str.gif")
    } yield {
      imageMap
          .composite(new AlphaComposite(1.0), imageWeather)
          .composite(new AlphaComposite(1.0), imageMsk)
    }
  }
}
