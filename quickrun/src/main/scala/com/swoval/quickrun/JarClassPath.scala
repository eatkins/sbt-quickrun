package com.swoval.quickrun
import java.net.URL
import java.nio.file.Path

case class JarClassPath(jars: Seq[Path], snapshots: Seq[(Path, Long)]) {
  lazy val urls: Array[URL] = (jars.map(_.toUri.toURL) ++ snapshots.map(_._1.toUri.toURL)).toArray
}
object JarClassPath {
  def apply(jars: Seq[Path]): JarClassPath = JarClassPath(jars, Nil)
}
