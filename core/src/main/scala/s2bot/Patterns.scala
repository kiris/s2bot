package s2bot

object Patterns {
  val user: String = "<@([^>]+)>"

  val channel: String = "<#(.+)\\|.*>"

  val atHear: String = "<!here>"

  val atChannel: String = "<!channel>"

  val atEveryone: String = "<!everyone>"
}
