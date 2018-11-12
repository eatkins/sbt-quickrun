package com.swoval.quickrun

import java.nio.file.Paths

import sbt.ScriptedPlugin.autoImport.scriptedBufferLog
import sbt.plugins.SbtPlugin
import sbt._
import sbt.Keys._
import Dependencies._
import sbt.internal.io.Source

import scala.collection.JavaConverters._

object Build {
  val genTestResourceClasses: TaskKey[Unit] = taskKey[Unit]("Generate test resource classes")
  private val baseVersion: String = "0.1.0-SNAPSHOT"
  private val (scala210, scala212) = ("2.10.7", "2.12.7")
  private val scalaCrossVersions = Seq(scala210, scala212)
  private val projectName = "sbt-quickrun"

  private def settings(args: Def.Setting[_]*): SettingsDefinition =
    Def.SettingsDefinition.wrapSettingsDefinition(args)
  private def commonSettings: SettingsDefinition =
    settings(
      scalaVersion in ThisBuild := scala212,
      organization := "com.swoval",
      homepage := Some(url(s"https://github.com/swoval/$projectName")),
      scmInfo := Some(
        ScmInfo(url(s"https://github.com/swoval/$projectName"),
                s"git@github.com:swoval/$projectName.git")),
      developers := List(
        Developer("username",
                  "Ethan Atkins",
                  "ethan.atkins@gmail.com",
                  url("https://github.com/eatkins"))),
      licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
      scalacOptions ++= Seq("-feature"),
      publishTo := {
        val p = publishTo.value
        if (sys.props.get("SonatypeSnapshot").fold(false)(_ == "true"))
          Some(Opts.resolver.sonatypeSnapshots): Option[Resolver]
        else if (sys.props.get("SonatypeRelease").fold(false)(_ == "true"))
          Some(Opts.resolver.sonatypeReleases): Option[Resolver]
        else p
      },
      version in ThisBuild := {
        val v = baseVersion
        if (sys.props.get("SonatypeSnapshot").fold(false)(_ == "true")) {
          if (v.endsWith("-SNAPSHOT")) v else s"$v-SNAPSHOT"
        } else {
          v
        }
      },
    )

  val benchmark: Project = project
    .enablePlugins(SbtPlugin)
    .settings(
      organization := "com.swoval",
      scalaVersion := scala210,
      version in ThisBuild := baseVersion,
      scriptedBufferLog := false,
      crossScalaVersions := scalaCrossVersions,
      sbtVersion in pluginCrossBuild := {
        if ((scalaVersion in crossVersion).value == scala210) "0.13.17" else "1.0.4"
      },
      skip in publish :=
        !version.value
          .endsWith("-SNAPSHOT") || !sys.props.get("SonatypeSnapshot").fold(true)(_ == "true"),
      crossSbtVersions := Seq("1.1.1", "0.13.17"),
      name := "sbt-swoval-benchmark",
      description := "Benchmark repeated invocations of main method."
    )

  val quickrunPlugin: Project = (project in file("quickrun"))
    .enablePlugins(SbtPlugin)
    .settings(
      commonSettings,
      publishLocal := {
        (benchmark / publishLocal).value
        publishLocal.value
      },
      scalaVersion := scala210,
      version in ThisBuild := baseVersion,
      scriptedBufferLog := false,
      crossScalaVersions := scalaCrossVersions,
      sbtVersion in pluginCrossBuild := {
        if ((scalaVersion in crossVersion).value == scala210) "0.13.16" else "1.0.4"
      },
      skip in publish :=
        !version.value
          .endsWith("-SNAPSHOT") || !sys.props.get("SonatypeSnapshot").fold(true)(_ == "true"),
      crossSbtVersions := Seq("1.0.4", "0.13.17"),
      name := projectName,
      description := "Minimize task start latency by caching loaded classes for external dependencies.",
      libraryDependencies ++= Seq(
        swovalReflectCore,
        utest
      ),
      genTestResourceClasses := Plugins.genTestResourceClasses.value,
      testFrameworks += new TestFramework("utest.runner.Framework"),
      watchSources += Source(
        baseDirectory.value / "src" / "sbt-test",
        ("test" | "*.sbt" | "*.scala") -- new FileFilter {
          override def accept(f: File): Boolean =
            f.toPath.iterator.asScala.toSeq.contains(Paths.get("target"))
        },
        NothingFilter
      )
    )

  val quickrun: Project = (project in file(".")).aggregate(benchmark, quickrunPlugin)
}
