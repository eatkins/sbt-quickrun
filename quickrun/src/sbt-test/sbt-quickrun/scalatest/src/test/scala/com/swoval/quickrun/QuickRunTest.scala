package com.swoval.quickrun

import org.scalatest.{ FlatSpec, Matchers }

class QuickRunTest extends FlatSpec with Matchers {
  "quickrun" should "exit" in {
    1 shouldBe 1
  }
}