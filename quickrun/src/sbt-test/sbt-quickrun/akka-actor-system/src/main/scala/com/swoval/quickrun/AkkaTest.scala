package com.swoval.quickrun

import java.nio.file.{ Files, Paths }
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._

object AkkaTest {
  def main(args: Array[String]): Unit = {
    val file = Paths.get(args.head)
    val start = System.nanoTime
    val system = ActorSystem("akka")
    Await.result(system.terminate(), 5.seconds)
    val end = System.nanoTime
    Files.write(file, (end - start).toString.getBytes)
    println(s"$file: ${new String(Files.readAllBytes(file))}")
  }
}
