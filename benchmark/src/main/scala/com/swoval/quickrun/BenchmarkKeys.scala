package com.swoval.quickrun

import sbt._

trait BenchmarkKeys {
  val benchmark = inputKey[Unit]("Benchmark a main method")
}
