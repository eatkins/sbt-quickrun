package com.swoval.quickrun

import java.lang.reflect.InvocationTargetException
import java.net.{ URL, URLClassLoader }
import java.util.concurrent.{ Callable, ConcurrentHashMap }

import scala.collection.JavaConverters._
import com.swoval.reflect.{ ChildFirstClassLoader, ClassLoaders, RequiredClassLoader, TaskRunner }
import sbt._
import Keys._

private[quickrun] object QuickrunImpl {
  lazy val runner = new TaskRunner(2)
  private val classLoaders =
    new ConcurrentHashMap[Set[URL], URLClassLoader].asScala

  private def getTask(className: String,
                      args: Seq[String],
                      cp: Seq[URL],
                      jars: Seq[URL]): Callable[Unit] =
    new Callable[Unit] {
      override def call(): Unit = {
        try {
          val parent =
            classLoaders.getOrElseUpdate(jars.toSet, new ChildFirstClassLoader(jars.toArray))
          val loader = new ChildFirstClassLoader(parent)
            .copy(cp.toArray)
            .copy(new java.util.function.Function[String, RequiredClassLoader] {
              override def apply(s: String): RequiredClassLoader =
                if (s.startsWith("com.swoval")) RequiredClassLoader.FORCE_CHILD
                else RequiredClassLoader.FORCE_PARENT
            })

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
    }

  def quickrun: Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = Def.spaceDelimited().parsed
    val main = (mainClass in Compile in run).value.getOrElse(
      throw new IllegalStateException(s"No main class found for ${projectID.value.name}"))
    val (jars, cp) =
      (fullClasspath in Compile in run).value
        .map(_.data.toURI.toURL)
        .partition(_.getFile.endsWith(".jar"))
    val task = getTask(main, args, cp, jars)
    try {
      val runningTask = runner.runTask(1, task, classOf[Unit], true)
      runningTask.getValue.get()
    } catch {
      case _: InterruptedException => println("Interrupted!")
      case e: Exception            => e.printStackTrace(System.err)
    }
  }
}
