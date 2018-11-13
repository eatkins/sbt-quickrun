package com.swoval.quickrun

import sbt._
import Compat.ScalaInstance
import com.swoval.reflect.{ ChildFirstClassLoader, ClassLoaders, RequiredClassLoader }

import scala.util.Try
import scala.util.control.NonFatal
import java8._

object Runner {
  class Run(instance: ScalaInstance,
            trapExit: Boolean,
            nativeTmp: File,
            classLoaderCache: ClassLoaderCache,
            projectJars: Seq[File])
      extends QuickrunRun(instance,
                          trapExit,
                          nativeTmp,
                          impl(instance, trapExit, nativeTmp, classLoaderCache, projectJars))
  private[this] def impl(
      instance: ScalaInstance,
      trapExit: Boolean,
      nativeTmp: File,
      classLoaderCache: ClassLoaderCache,
      projectJars: Seq[File]): (String, Seq[File], Seq[String], Logger) => Try[Unit] =
    (mainClass, classpath, options, log) => {
      def run0(
          mainClassName: String,
          classpath: Seq[File],
          options: Seq[String],
          log: Logger
      ): Unit = {
        log.debug("  Classpath:\n\t" + classpath.mkString("\n\t"))
        val initLoader = Thread.currentThread.getContextClassLoader
        val projectJarNames = projectJars.map(_.getName).toSet
        try {
          val cp = JarClassPath(classpath.collect {
            case f if !projectJarNames.contains(f.getName) => f.toPath
          })
          println(s"WTF $cp\n$projectJars")
          val parent = classLoaderCache.get(cp)
          println(s"WTF loader $parent")
          val loader = new ChildFirstClassLoader(projectJars.map(_.toURI.toURL).toArray,
                                                 _ => RequiredClassLoader.FORCE_CHILD,
                                                 parent)
          Thread.currentThread.setContextClassLoader(loader)
          ClassLoaders.invokeStaticMethod(loader, mainClassName, "main", options.toArray)
        } finally Thread.currentThread.setContextClassLoader(initLoader)
      }
      log.info("Running " + mainClass + " " + options.mkString(" "))

      def execute() =
        try {
          run0(mainClass, classpath, options, log)
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
