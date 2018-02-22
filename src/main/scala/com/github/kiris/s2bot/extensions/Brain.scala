package com.github.kiris.s2bot.extensions

import scala.collection.concurrent.{Map, TrieMap}
abstract class Brain {
  protected val data: Map[String, Any] = TrieMap()


}
