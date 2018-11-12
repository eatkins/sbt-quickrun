package com.swoval.quickrun

import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._

object AkkaTest {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("akka")
    Await.result(system.terminate(), 5.seconds)
  }
}
