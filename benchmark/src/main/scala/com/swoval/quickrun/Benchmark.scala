package com.swoval.quickrun

import sbt._

object Benchmark extends AutoPlugin {
  override val trigger = AllRequirements
  object autoImport extends BenchmarkKeys
  import autoImport._
  override lazy val projectSettings = super.projectSettings ++ Seq(
    benchmark := BenchmarkImpl.benchmark
  )
}
