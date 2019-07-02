package s2bot.plugins.amesh

import java.time.LocalDateTime

import akka.actor.ActorSystem
import org.scalatest.AsyncFlatSpec


class AmeshTest extends AsyncFlatSpec {

  private implicit val actorSystem: ActorSystem = ActorSystem()

  "amesh" should "be" in {
    new Amesh().amesh(LocalDateTime.now()).map { _ =>
      succeed
    }
  }
}
