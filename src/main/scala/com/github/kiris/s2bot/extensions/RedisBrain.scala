package com.github.kiris.s2bot.extensions
import com.github.kiris.s2bot.S2Bot
import redis.{ByteStringDeserializer, ByteStringSerializer, RedisClient}

import scala.concurrent.Future

class RedisBrain(client: RedisClient, redisKey: String = "s2bot:brain") {
  def get[A : ByteStringDeserializer](key: String): Future[Option[A]] = client.hget(redisKey, key)

  def set[A : ByteStringSerializer](key: String, value: A): Future[Boolean] = client.hset(redisKey, key, value)
}

object RedisBrain {
  object Implicts {
    implicit class S2BotSyntax(s2bot: S2Bot) {
      def brain(implicit redisBrain: RedisBrain): RedisBrain = redisBrain
    }
  }
}