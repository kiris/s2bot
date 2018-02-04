package com.github.kiris.s2bot.extensions
import com.github.kiris.s2bot.S2Bot
import com.redis._
import com.redis.serialization.{Format, Parse}

class RedisBrain(client: RedisClient, redisKey: String = "s2bot:brain") {
  def get[A](key: String)(implicit format: Format, parser: Parse[A]): Option[A] = client.hget[A](redisKey, key)

  def set(key: String, value: Any)(implicit format: Format): Boolean = client.hset(redisKey, key, value)
}

object RedisBrain {
  object Implicts {
    implicit class S2BotSyntax(s2bot: S2Bot) {
      def brain(implicit redisBrain: RedisBrain): RedisBrain = redisBrain
    }
  }
}