package com.swoval.quickrun

import java.lang.reflect.{ Method, Modifier }

import sbt._
import Compat.ScalaInstance
import com.swoval.reflect.{ ChildFirstClassLoader, ClassLoaders }

import scala.util.Try
import scala.util.control.NonFatal

object Runner {
  class Run(instance: ScalaInstance, trapExit: Boolean, nativeTmp: File)
      extends QuickrunRun(instance, trapExit, nativeTmp, impl(instance, trapExit, nativeTmp))
  private[this] def impl(instance: ScalaInstance,
                         trapExit: Boolean,
                         nativeTmp: File): (String, Seq[File], Seq[String], Logger) => Try[Unit] =
    (mainClass, classpath, options, log) => {
      def run0(
          mainClassName: String,
          classpath: Seq[File],
          options: Seq[String],
          log: Logger
      ): Unit = {
        log.debug("  Classpath:\n\t" + classpath.mkString("\n\t"))
        val initLoader = Thread.currentThread.getContextClassLoader
        try {
          val loader = new ChildFirstClassLoader(classpath.map(_.toURI.toURL).toArray)
          Thread.currentThread.setContextClassLoader(loader)
          ClassLoaders.invokeStaticMethod(loader, mainClassName, "main", options.toArray)
        } finally Thread.currentThread.setContextClassLoader(initLoader)
      }
      log.info("Running " + mainClass + " " + options.mkString(" "))

      def execute() =
        try {
          println(s"WTF cp: ${classpath mkString "\n"}"); run0(mainClass, classpath, options, log)
        } catch {
          case e: java.lang.reflect.InvocationTargetException => throw e.getCause
        }
      def directExecute(): Try[Unit] =
        Try(execute()) recover {
          case NonFatal(e) =>
            // bgStop should not print out stack trace
            // log.trace(e)
            throw e
        }
      // try { execute(); None } catch { case e: Exception => log.trace(e); Some(e.toString) }

      if (trapExit) Run.executeTrapExit(execute(), log)
      else directExecute()
    }
}
