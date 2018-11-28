# s2bot
Slack Bot framework for the Scala Language.


# Example

```scala
import com.typesafe.config.ConfigFactory
import com.github.kiris.s2bot.buildin.Helpable.Usage
import com.github.kiris.s2bot.{S2Bot, Script}

object Ping extends Script with Helpable {

  override def apply(bot: S2Bot): Unit = {
    bot.respond {
      case ("ping", message) =>
        bot.reply(message, "pong")
    }
  }

  override def usage(bot: S2Bot): Usage = Usage(
    commands = List(
      s"${bot.me} ping - pong"
    )
  )
}

object Main {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    
    val bot = new S2Bot(
      scripts = List(
        com.github.kiris.s2bot.buildin.Help,
        Ping
      ),
      token = "$SLACK_TOKEN%",
      config = config,
      duration = 30.seconds
    )

    bot.run()
  }
}
