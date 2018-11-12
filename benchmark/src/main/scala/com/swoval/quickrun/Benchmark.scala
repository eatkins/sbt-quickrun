package com.swoval.quickrun

import sbt._
import Keys._

object Benchmark extends AutoPlugin {
  override val trigger = AllRequirements
  object autoImport extends BenchmarkKeys
  override lazy val globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    onLoad := { state =>
      val newState = onLoad.value(state)
      newState.copy(definedCommands = newState.definedCommands :+ BenchmarkImpl.command)
    }
  )
}
