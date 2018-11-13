package com.swoval.quickrun

import java.lang.reflect.{ Method, Modifier }

import sbt._
import xsbti.compile.ScalaInstance

import scala.util.Try
import scala.util.control.NonFatal

object Runner {
  class Run(instance: ScalaInstance, trapExit: Boolean, nativeTmp: File)
      extends QuickrunRun(impl(instance, trapExit, nativeTmp))
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
        val loader = Thread.currentThread.getContextClassLoader
        val main = getMainMethod(mainClassName, loader)
        invokeMain(loader, main, options)
      }
      def invokeMain(loader: ClassLoader, main: Method, options: Seq[String]): Unit = {
        val currentThread = Thread.currentThread
        val oldLoader = Thread.currentThread.getContextClassLoader
        currentThread.setContextClassLoader(loader)
        try { main.invoke(null, options.toArray[String]); () } finally {
          currentThread.setContextClassLoader(oldLoader)
        }
      }
      def getMainMethod(mainClassName: String, loader: ClassLoader) = {
        val mainClass = Class.forName(mainClassName, true, loader)
        val method = mainClass.getMethod("main", classOf[Array[String]])
        // jvm allows the actual main class to be non-public and to run a method in the non-public class,
        //  we need to make it accessible
        method.setAccessible(true)
        val modifiers = method.getModifiers
        if (!Modifier.isPublic(modifiers))
          throw new NoSuchMethodException(mainClassName + ".main is not public")
        if (!Modifier.isStatic(modifiers))
          throw new NoSuchMethodException(mainClassName + ".main is not static")
        method
      }
      log.info("Running " + mainClass + " " + options.mkString(" "))

      def execute() =
        try { run0(mainClass, classpath, options, log) } catch {
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
