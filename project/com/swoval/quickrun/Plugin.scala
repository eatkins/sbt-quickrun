package com.swoval.quickrun

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ Callable, ConcurrentHashMap, TimeUnit }

import com.swoval.reflect.{ ChildFirstClassLoader, ClassLoaders, RequiredClassLoader, TaskRunner }
import sbt.Keys._
import sbt.{ Task => _, _ }

import scala.collection.JavaConverters._

object Plugin {
  lazy val runner = new TaskRunner(2)
  private val classLoaders =
    new ConcurrentHashMap[Set[URL], ChildFirstClassLoader].asScala

  private def getTask(className: String,
                      args: Seq[String],
                      cp: Seq[URL],
                      jars: Seq[URL]): Callable[Unit] =
    () => {
      try {
        val start = System.nanoTime
        val parent =
          classLoaders.getOrElseUpdate(jars.toSet, new ChildFirstClassLoader(jars.toArray))
        val loader = new ChildFirstClassLoader(parent)
          .copy(cp.toArray)
          .copy((s: String) =>
            if (s.startsWith("com.example")) RequiredClassLoader.FORCE_CHILD
            else RequiredClassLoader.FORCE_PARENT)

        val initLoader = Thread.currentThread.getContextClassLoader
        Thread.currentThread.setContextClassLoader(loader)
        try {
          ClassLoaders
            .invokeStaticMethod(loader, className, "main", args.toArray)
        } catch {
          case _: Exception =>
            ClassLoaders.invokeMethod(ClassLoaders
                                        .getStaticField(loader, className + "$", "MODULE$"),
                                      "main",
                                      args.toArray)
        } finally {
          Thread.currentThread.setContextClassLoader(initLoader)
        }
      } catch {
        case e: InvocationTargetException =>
          System.err.println(s"Error running $className")
          e.getCause.printStackTrace(System.err)
        case e: Exception =>
          System.err.println(s"Error running $className")
          e.printStackTrace(System.err)
      }
      ()
    }

  def quickrun = Def.inputTask {
    val args = Def.spaceDelimited().parsed
    val main = (mainClass in Compile in run).value.getOrElse(
      throw new IllegalStateException(s"No main class found for ${projectID.value.name}"))
    val (cp, jars) =
      (fullClasspath in Compile in run).value
        .map(_.data.toURI.toURL)
        .partition(_.getFile.endsWith(".jar"))
    val task = getTask(main, args, jars, cp)
    try {
      val runningTask = runner.runTask(1, task, classOf[Unit], true)
      runningTask.getValue.get()
    } catch {
      case e: InterruptedException =>
      case e: Exception            => e.printStackTrace(System.err)
    }
  }
}
