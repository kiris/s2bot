package s2bot.plugins.uranai

import java.time.{MonthDay, ZonedDateTime}
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import dispatch.{Http, as, url}
import io.circe.parser._
import io.circe.generic.auto._
import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.{DefaultKeys, Usage}
import s2bot.{Plugin, S2Bot}
import slack.models.Message

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class Uranai(
    birthDayProvider: Message => Future[Option[MonthDay]] = _ => Future.successful(None)
)(implicit system: ActorSystem) extends Plugin with Helpable {

  import Uranai._

  implicit private val ec: ExecutionContext = system.dispatcher

  override def usage(bot: S2Bot): Usage =
    Usage(
      DefaultKeys.COMMANDS -> List(
        s"uranai <MMDD> - 誕生日の星座占いの結果を表示します"
      )
    )

  override def apply(bot: S2Bot): S2Bot = {
    def star(n: Int): String = {
      "★" * n + "☆" * (5 - n)
    }

    def uranai(msg: Message, birthDay: MonthDay): Future[Unit] = {
      val now = ZonedDateTime.now()
      for {
        horoscope <- getHoroscope(now, birthDay)
        _ <- bot.say(msg, s"""```
                             |${horoscope.rank}位 ${horoscope.sign}
                             |総合: ${star(horoscope.total)}
                             |恋愛運: ${star(horoscope.love)}
                             |金運: ${star(horoscope.money)}
                             |仕事運: ${star(horoscope.job)}
                             |ラッキーカラー: ${horoscope.color}
                             |ラッキーアイテム: ${horoscope.item}
                             |${horoscope.content}
                             |```""".stripMargin)
      } yield ()
    }

    bot.hear {
      case ("uranai", msg) =>
        for {
          birthDayOpt <- birthDayProvider(msg)
          _ <- birthDayOpt match {
            case Some(birthDay) =>
              uranai(msg, birthDay)

            case None =>
              Future.unit
          }
        } yield ()

      case (URANAI_PATTERN(month, day), msg) =>
        val birthDay = MonthDay.of(month.toInt, day.toInt)
        uranai(msg, birthDay)
    }
  }


  def getHoroscope(now: ZonedDateTime, birthday: MonthDay): Future[Horoscope] = {
    for {
      horoscopes <- request(now)
    } yield {
      val constellation = Constellation.find(birthday)
      horoscopes(Constellation.index(constellation))
    }
  }

  private def request(now: ZonedDateTime): Future[List[Horoscope]] = {
    val today = now.format(DateTimeFormatter.ISO_DATE)
    val req = url(s"http://api.jugemkey.jp/api/horoscope/free/$today")
    Http.default(req OK as.String).flatMap { body =>
      Future.fromTry {
        for {
          json <- parse(body).toTry
          horoscopes <- json.hcursor.downField("horoscope").downField(today).as[List[Horoscope]].toTry
        } yield horoscopes
      }
    }
  }
}

object Uranai {

  val URANAI_PATTERN: Regex = "uranai\\s*(\\d\\d?)[ /-]?(\\d\\d?)".r

  case class Horoscope(
      total: Int,
      love: Int,
      money: Int,
      job: Int,
      rank: Int,
      item: String,
      color: String,
      content: String,
      sign: String
  )

  sealed abstract class Constellation(val name: String, val start: MonthDay, val end: MonthDay)
  object Constellation {
    case object Capricorn extends Constellation("山羊座", MonthDay.of(12, 22), MonthDay.of(1, 19))
    case object Aquarius extends Constellation("水瓶座", MonthDay.of(1, 20), MonthDay.of(2, 18))
    case object Pisces extends Constellation("魚座", MonthDay.of(2, 19), MonthDay.of(3, 20))
    case object Aries extends Constellation("牡羊座", MonthDay.of(3, 21), MonthDay.of(4, 19))
    case object Taurus extends Constellation("牡牛座", MonthDay.of(4, 20), MonthDay.of(5, 20))
    case object Gemini extends Constellation("双子座", MonthDay.of(5, 21), MonthDay.of(6, 21))
    case object Cancer extends Constellation("蟹座", MonthDay.of(6, 22), MonthDay.of(7, 22))
    case object Leo extends Constellation("獅子座", MonthDay.of(7, 23), MonthDay.of(8, 22))
    case object Virgo extends Constellation("乙女座", MonthDay.of(8, 23), MonthDay.of(9, 22))
    case object Libra extends Constellation("天秤座", MonthDay.of(9, 23), MonthDay.of(10, 23))
    case object Scorpio extends Constellation("蠍座", MonthDay.of(10, 24), MonthDay.of(11, 22))
    case object Sagittarius extends Constellation("射手座", MonthDay.of(11, 23), MonthDay.of(12, 21))

    val all: Seq[Constellation] = Seq(
      Capricorn, Aquarius, Pisces, Aries, Taurus, Gemini, Cancer, Leo, Virgo, Libra, Scorpio, Sagittarius
    )

    def find(name: String): Option[Constellation] = {
      all.find(_.name == name)
    }

    def find(monthDay: MonthDay): Constellation = {
      all.find(!_.end.isBefore(monthDay)).getOrElse(Capricorn)
    }

    def index(elem: Constellation): Int = {
      all.indexOf(elem)
    }
  }
}
