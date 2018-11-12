package com.swoval.quickrun.scripted

import sbt._

object Build {
  val extraSettings: Seq[Def.Setting[_]] = com.swoval.quickrun.QuickrunPlugin.allSettings
}
