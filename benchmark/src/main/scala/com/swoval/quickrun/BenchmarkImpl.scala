package com.swoval.quickrun

import sbt._
import Keys._

object BenchmarkImpl {
  def benchmark: Def.Initialize[InputTask[Unit]] = Def.inputTaskDyn {
    val threshold = java.lang.Double.valueOf(Def.spaceDelimited().parsed.head)
    val s = state
    println(s)
//    println(Project.extract(s).structure.data)
//    Def.task(())

//    Def.sequential(
//      quickrun
//    )
    Def.task(())
  }
}
