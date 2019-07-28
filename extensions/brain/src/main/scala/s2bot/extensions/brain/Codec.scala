package s2bot.extensions.brain

trait Codec[A, E] {
  def encode(value: A): E
  def decode(value: E): A
}