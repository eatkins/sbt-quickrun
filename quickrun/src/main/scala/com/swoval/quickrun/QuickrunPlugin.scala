package com.swoval.quickrun

import java.nio.file.Files

import com.swoval.quickrun.java8._ // This is necessary for scala 2.10 source compatibility.
import com.swoval.reflect.{ ChildFirstClassLoader, RequiredClassLoader }
import sbt.Keys._
import sbt._

object QuickrunPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  object autoImport extends QuickrunKeys
  import autoImport._
  private[this] val classLoaderCache = new ClassLoaderCache(16)

  val testSettings: Seq[Def.Setting[_]] = inConfig(Test)(
    Seq(
      testLoader := {
        val cp = fullClasspath.value.map(_.data.toPath)
        val (allJars, directories) = cp.partition(Files.isRegularFile(_))
        val (snapshots, jars) = allJars.partition(_.getFileName.toString.endsWith("-SNAPSHOT"))
        val jarClassPath =
          JarClassPath(jars, snapshots.map(f => f -> Files.getLastModifiedTime(f).toMillis))
        val parent = classLoaderCache.get(jarClassPath)
        new ChildFirstClassLoader(
          directories.map(_.toUri.toURL).toArray,
          (name: String) =>
            if (name.startsWith("com.swoval")) RequiredClassLoader.FORCE_CHILD
            else RequiredClassLoader.FORCE_PARENT,
          parent
        )
      }
    ))
  val compileSettings: Seq[Def.Setting[_]] = Seq(
    runner := {
      val original = runner.value
      original match {
        case r: sbt.Run =>
          val (scalaInstance, trapExit, tmpFile) = Reflection.runParams(r)
          new Runner.Run(scalaInstance, trapExit, tmpFile)
        case r => r
      }
    }
  )
  val combinedCompileSettings: Seq[Def.Setting[_]] = inConfig(Compile)(compileSettings) ++ inConfig(
    Runtime)(compileSettings)
  val sharedSettings: Seq[Def.Setting[_]] = Seq(
    quickrun := QuickrunImpl.quickrun.evaluated
  )
  val allSettings: Seq[Def.Setting[_]] = inConfig(Compile)(sharedSettings) ++ inConfig(Test)(
    sharedSettings) ++ testSettings ++ combinedCompileSettings
  override lazy val projectSettings: Seq[Def.Setting[_]] = super.projectSettings ++ allSettings
  //override lazy val globalSettings: Seq[Def.Setting[_]] = super.globalSettings ++ compileSettings
}
