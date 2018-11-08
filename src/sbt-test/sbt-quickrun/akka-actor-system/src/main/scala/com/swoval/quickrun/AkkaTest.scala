package com.swoval.quickrun

import java.nio.file.Paths
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._

object AkkaTest {
  def main(args: Array[String]): Unit = {
    //val file = Paths.get(args)
    val system = ActorSystem("akka")
    Await.result(system.terminate(), 5.seconds)
    println("Ran!")
  }
}
