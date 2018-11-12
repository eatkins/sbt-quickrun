package com.swoval.quickrun

import java.net.URL
import java.nio.file.{ Files, Path, Paths, StandardCopyOption }
import scala.collection.JavaConverters._

object TestHelpers {
  private val quickrunDir = Paths.get("").toAbsolutePath match {
    case p if p.getParent.toString != "quickrun" => p.resolve("quickrun")
    case p                                       => p
  }
  val classPathUrls: Array[URL] = Seq("classes", "test-classes").map { dir =>
    quickrunDir.resolve(s"target/$dir").toUri.toURL
  }.toArray
  val resourceDir: Path = quickrunDir.resolve("src/test/resources/classes")
  def copy(path: Path, target: Path): Unit = {
    Files.walk(path).iterator.asScala.filter(!Files.isDirectory(_)).foreach { f =>
      val relative = path.relativize(f)
      println(f)
      Files.createDirectories(target.resolve(relative.getParent))
      Files.copy(f, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING)
    }
  }
  def deleteRecursive(path: Path): Unit = {
    Files.walk(path).iterator.asScala.toIndexedSeq.reverse.foreach(Files.deleteIfExists)
  }
  def withTempDirectory[R](f: Path => R): R = {
    val tempDirectory = Files.createTempDirectory("com.swoval.reflect.test")
    try f(tempDirectory)
    finally deleteRecursive(tempDirectory)
  }
}
