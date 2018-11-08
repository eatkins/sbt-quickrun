package com.swoval.quickrun

import sbt.{ Def, _ }

object QuickrunPlugin extends AutoPlugin {
  override def trigger = allRequirements
  object autoImport extends QuickrunKeys
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = super.projectSettings ++ Seq(
    quickrun := QuickrunImpl.quickrun.evaluated
  )
}
