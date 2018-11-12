package com.swoval.quickrun

import java.io.File
import java.nio.file.{ Files, Path, Paths, StandardCopyOption }
import java.util.concurrent.TimeUnit

import sbt._
import Keys._

import scala.tools.nsc
import scala.tools.nsc.reporters.StoreReporter
import scala.collection.JavaConverters._

object Plugins {
  private def withCompiler[R](classpath: String, outputDir: String)(f: nsc.Global => R): R = {
    val settings = new nsc.Settings()
    settings.bootclasspath.value = classpath
    settings.classpath.value = classpath
    settings.outputDirs.add(outputDir, outputDir)
    f(nsc.Global(settings, new StoreReporter))
  }
  def classPath(config: ConfigKey): Def.Initialize[Task[String]] = Def.task {
    (fullClasspath in config).value
      .map(_.data)
      .mkString(File.pathSeparator)
  }
  def copy(path: Path, target: Path): Unit =
    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
  def genTestResourceClasses: Def.Initialize[Task[Unit]] = Def.task {
    val cp = classPath(Test).value

    IO.withTemporaryDirectory { dir =>
      val path = dir.toPath
      val resourceDir = (resourceDirectory in Test).value.toPath
      val classResourceDir: Int => Path =
        i => resourceDir.resolve(s"classes/$i/com/swoval/quickrun")
      withCompiler(cp, path.toString) { g =>
        val file = path.resolve("TestModule.scala").toFile
        (1 to 2) foreach { i =>
          IO.write(
            file,
            s"""
             |package com.swoval.quickrun
             |
             |object TestClasses {
             |  class Foo
             |  class Bar extends Foo
             |  class Buzz extends Foo {
             |    def x: Int = 2 + $i
             |  }
             |  class Parent extends Foo {
             |    def y: Int = 2 + $i
             |  }
             |}
             |
             |import TestClasses._
             |
             |object TestModule {
             |  def static: Int = $i
             |  def bar(foo: Foo): Int = $i
             |}
           """.stripMargin
          )
          // If compile fails, it won't throw an exception.
          new g.Run().compile(List(file.toString))
          val outputDir = classResourceDir(i)
          Files.createDirectories(outputDir)
          Files
            .walk(path)
            .iterator
            .asScala
            .filter(_.getFileName.toString.endsWith(".class"))
            .foreach { f =>
              copy(f, outputDir.resolve(f.getFileName))
            }
          val proc = new ProcessBuilder("jar", "-cvf", "quickrun-test.jar", "com/")
            .directory(resourceDir.resolve(s"classes/$i").toFile)
            .inheritIO()
            .start()
          assert(proc.waitFor(5, TimeUnit.SECONDS))
        }
      }
    }
  }
}
