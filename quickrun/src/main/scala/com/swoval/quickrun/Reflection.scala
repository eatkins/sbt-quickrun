package com.swoval.quickrun

import sbt._

object Reflection {
  val runParams: sbt.Run => (Compat.ScalaInstance, Boolean, File) = {
    val clazz = classOf[sbt.Run]
    val fields @ Array(scalaInstance, trapExit, nativeTmp) = clazz.getDeclaredFields
    fields.foreach(_.setAccessible(true))
    run: sbt.Run =>
      (scalaInstance.get(run).asInstanceOf[Compat.ScalaInstance],
       trapExit.get(run).asInstanceOf[Boolean],
       nativeTmp.get(run).asInstanceOf[File])
  }
}
