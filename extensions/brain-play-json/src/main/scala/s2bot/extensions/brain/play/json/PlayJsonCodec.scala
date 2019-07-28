package s2bot.extensions.brain.play.json

import akka.util.ByteString
import play.api.libs.json.{Json, Reads, Writes}
import s2bot.extensions.brain.Codec

object PlayJsonCodec {
  implicit def stringCodec[A](implicit reads: Reads[A], writes: Writes[A]): Codec[A, String] = new Codec[A, String] {
    override def encode(value: A): String = Json.toJson(value).toString

    override def decode(value: String): A = Json.parse(value).as[A]
  }

  implicit def byteStringCodec[A](implicit reads: Reads[A], writes: Writes[A]): Codec[A, ByteString] = new Codec[A, ByteString] {
    override def encode(value: A): ByteString = ByteString(Json.toJson(value).toString)

    override def decode(value: ByteString): A = Json.parse(value.utf8String).as[A]
  }
}
