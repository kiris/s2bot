package s2bot

import scala.concurrent.Future

trait Script {
  def apply(bot: S2Bot): Future[Any]
}
