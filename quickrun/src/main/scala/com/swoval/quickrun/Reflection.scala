package com.swoval.quickrun

import sbt._

object Reflection {
  val runParams: sbt.Run => (ScalaInstance, Boolean, File) = {
    val clazz = classOf[sbt.Run]
    val fields @ Array(scalaInstance, trapExit, nativeTmp) = clazz.getDeclaredFields
    fields.foreach(_.setAccessible(true))
    run: sbt.Run =>
      (scalaInstance.get(run).asInstanceOf[ScalaInstance],
       trapExit.get(run).asInstanceOf[Boolean],
       nativeTmp.get(run).asInstanceOf[File])
  }
}
