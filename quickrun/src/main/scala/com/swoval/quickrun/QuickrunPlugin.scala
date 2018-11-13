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
    runner in run := {
      val original = runner.value
      val transitiveJars = transitiveProjectJars.value
      original match {
        case r: sbt.Run =>
          val (scalaInstance, trapExit, tmpFile) = Reflection.runParams(r)
          new Runner.Run(scalaInstance, trapExit, tmpFile, classLoaderCache, transitiveJars)
        case r => r
      }
    }
  )
  val combinedCompileSettings: Seq[Def.Setting[_]] = inConfig(Compile)(compileSettings) ++ inConfig(
    Runtime)(compileSettings)
  private def transitiveJars(conf: Configuration): Def.Initialize[Task[Seq[File]]] = Def.taskDyn {
    import sbt.Wrappers._
    val selectDeps = ScopeFilter(inAggregates(ThisProject) || inDeps(ThisProject))
    val allJars = (packageBin in conf).all(selectDeps)
    Def.task { allJars.value }
  }
  val sharedSettings: Seq[Def.Setting[_]] = Seq(
    quickrun := QuickrunImpl.quickrun.evaluated
  )
  val jars: Seq[Def.Setting[_]] = inConfig(Compile)(
    transitiveProjectJars := transitiveJars(Compile).value) ++
    inConfig(Test)(
      transitiveProjectJars := transitiveJars(Compile).value ++ transitiveJars(Test).value)
  val allSettings: Seq[Def.Setting[_]] = inConfig(Compile)(sharedSettings) ++ inConfig(Test)(
    sharedSettings) ++ testSettings ++ combinedCompileSettings ++ jars
  override lazy val projectSettings: Seq[Def.Setting[_]] = super.projectSettings ++ allSettings
  //override lazy val globalSettings: Seq[Def.Setting[_]] = super.globalSettings ++ compileSettings
}
