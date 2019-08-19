package s2bot.plugins.choice

import s2bot.plugins.buildin.Helpable
import s2bot.plugins.buildin.Helpable.DefaultKeys
import s2bot.{S2Bot, Plugin}
import slack.models.Message

import scala.concurrent.Future
import scala.util.Random
import scala.util.matching.Regex

class Choice() extends Plugin with Helpable {
  override def usage(bot: S2Bot): Helpable.Usage = Helpable.Usage(
    DefaultKeys.COMMANDS -> List(
      s"choice A B C ... - 指定された単語から1個をランダムに選びます",
      s"choice{N} A B C ... - 指定された単語からN個をランダムに選びます",
      s"shuffle A B C ... - 指定された単語をランダムに並べ替えます",
    )
  )

  override def apply(bot: S2Bot): S2Bot = {
    bot.hear {
      case (SHUFFLE_PATTERN(words), msg) =>
        shuffle(bot, words, msg)

      case (CHOICE_PATTERN(words), msg) =>
        choice(bot, 1, words, msg)

      case (CHOICE_N_PATTERN(n, words), msg) =>
        choice(bot, n.toInt, words, msg)
    }
  }

  private def choice(bot: S2Bot, n: Int, words: String, msg: Message): Future[Long] = {
    val candidates = parse(words)
    val choices = Random.shuffle(candidates).take(n).mkString(" ")
    bot.say(msg, s"$choices を選んだよ")
  }

  private def shuffle(bot: S2Bot, words: String, msg: Message): Future[Long] = {
    val items = parse(words)
    val shuffled = Random.shuffle(items).mkString(" ")
    bot.say(msg, s"$shuffled にシャッフルしたよ")
  }

  private def parse(words: String): Seq[String] = {
    words.split(" ").toList.filter(_.nonEmpty)
  }

  protected val CHOICE_PATTERN: Regex = "choice (.+)".r

  protected val CHOICE_N_PATTERN: Regex = "choice([1-9][0-9]*) (.+)".r

  protected val SHUFFLE_PATTERN: Regex = "shuffle (.+)".r
}
