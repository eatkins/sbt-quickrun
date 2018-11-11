package com.swoval.quickrun

import sbt._

trait QuickrunKeys {
  val quickrun = inputKey[Unit]("Run a task using a potentially cached ClassLoader.")
}
