package com.swoval.quickrun

import sbt._
import scala.util.{ Failure, Success, Try }

object Compat {
  type ScalaInstance = sbt.internal.inc.ScalaInstance
}

object Run {
  def executeTrapExit(execute: => Unit, log: Logger): Try[Unit] =
    sbt.Run.executeTrapExit(execute, log)
}

class QuickrunRun(instance: sbt.internal.inc.ScalaInstance,
                  trapExit: Boolean,
                  nativeTmp: File,
                  private[this] val impl: (String, Seq[File], Seq[String], Logger) => Try[Unit])
    extends sbt.Run(instance, trapExit, nativeTmp) {
  override def run(mainClass: String,
                   classpath: Seq[File],
                   options: Seq[String],
                   log: Logger): Try[Unit] = impl(mainClass, classpath, options, log)
}
