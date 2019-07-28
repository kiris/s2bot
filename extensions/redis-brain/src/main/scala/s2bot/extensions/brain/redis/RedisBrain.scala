package s2bot.extensions.brain.redis

import akka.util.ByteString
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClient}
import s2bot.extensions.brain.{Brain, Codec}

import scala.concurrent.Future
import RedisBrain._

class RedisBrain(client: RedisClient, redisKey: String = "s2bot:brain") extends Brain[ByteString] {

  override def get[A](key: String)(implicit reads: Codec[A, ByteString]): Future[Option[A]] = {
    client.hget[A](redisKey, key)
  }

  override def set[A](key: String, value: A)(implicit writes: Codec[A, ByteString]): Future[Boolean] = {
    client.hset[A](redisKey, key, value)
  }
}

private object RedisBrain {
  implicit def readsToByteStringDeserializer[A](implicit reads: Codec[A, ByteString]): ByteStringDeserializer[A] = (bs: ByteString) => reads.decode(bs)

  implicit def writesToByteStringSerializer[A](implicit writes: Codec[A, ByteString]): ByteStringSerializer[A] = (a: A) => writes.encode(a)
}