package s2bot.plugins.uranai

import org.scalatest.WordSpec

class UranaiTest extends WordSpec {

  "regex" should {
    "be" in {
      assert("uranai 1231".matches(Uranai.URANAI_PATTERN.regex))
      assert("uranai 0101".matches(Uranai.URANAI_PATTERN.regex))
      assert("uranai 2 2".matches(Uranai.URANAI_PATTERN.regex))
      assert("uranai 11/11".matches(Uranai.URANAI_PATTERN.regex))
      assert("uranai 3-3".matches(Uranai.URANAI_PATTERN.regex))
    }
  }

}
