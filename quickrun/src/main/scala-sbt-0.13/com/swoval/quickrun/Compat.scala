package com.swoval.quickrun

import sbt._
import scala.util.{ Failure, Success, Try }

object Compat {
//  val testLoader = inConfig(Test) {
//  }
}

object Run {
  def executeTrapExit(execute: => Unit, log: Logger): Try[Unit] = sbt.Run.executeTrapExit(execute, log) match {
    case Some(_) => Success(())
    case None => Failure(new IllegalArgumentException("Couldn't run task"))
  }
}

class QuickrunRun(private[this] val impl: (String, Seq[File], Seq[String], Logger) => Try[Unit]) extends ScalaRun {
  override def run(mainClass: String,
                   classpath: Seq[File],
                   options: Seq[String],
                   log: Logger): Option[String] = impl(mainClass, classpath, options, log) match {
    case _: Success[_] => None
    case Failure(e) => Some(e.getMessage)
  }
}
