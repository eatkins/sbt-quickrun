package com.swoval.quickrun

import java.nio.file.{ Files, Paths }
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import sbt._
import BasicCommands.otherCommandParser
import BasicCommandStrings.FailureWall

object BenchmarkImpl {
  private[this] val startTimes = new ConcurrentHashMap[String, Long].asScala
  private[this] val elapsed = new ConcurrentHashMap[String, Long].asScala
  def command = Command("benchmark")(otherCommandParser) { (state, argString) =>
    val args = argString.split(" ").filter(_.trim.nonEmpty).toSeq
    val cmd = args.head
    val iterations = args.tail.head.toInt
    val threshold = args.tail.tail.head.toDouble
    val setStart = Command("setStart")(otherCommandParser) { (state, argString) =>
      val now = System.nanoTime
      startTimes += argString.trim -> now
      state
    }
    val setElapsed = Command("setElapsed")(otherCommandParser) { (state, argString) =>
      val now = System.nanoTime
      elapsed += argString.trim -> (now - startTimes(argString))
      state
    }
    val dump = Command.command("dump") { state =>
      state
    }
    val checkThreshold = Command("checkThreshold")(otherCommandParser) { (state, argString) =>
      val Array(firstUUID, secondUUID) = argString.split(" ").map(_.trim).filter(_.nonEmpty)
      val firstElapsed = elapsed(firstUUID)
      val secondElapsed = elapsed(secondUUID)
      val ratio = (1.0 * secondElapsed) / firstElapsed
      if (ratio > threshold) {
        System.err.println(
          s"The ratio of the first task run time to the second ($ratio) was greater than allowed limit: $threshold")
        state.fail
      } else {
        System.out.println(
          s"The ratio of the first task run time to the second ($ratio) was within the allowed limit: $threshold")
        state
      }
    }
    val names = Set("setStart", "setElapsed", "clearCommands", "dump", "checkThreshold")
    val clearCommands = Command.command("clearCommands") { state =>
      startTimes.clear()
      elapsed.clear()
      state.copy(definedCommands = state.definedCommands.filterNot {
        case NamedCommand(s) => names(s.name)
        case _               => false
      })
    }
    val uuids = (1 to iterations) map (_ => UUID.randomUUID.toString)
    val newCommands = Seq(setStart, setElapsed, clearCommands, dump, checkThreshold)
    val lastOutput = Files.createTempFile("sbt-benchmark", ".out")
    val newState = s"checkThreshold ${uuids.head} ${uuids.last}" :: FailureWall :: "clearCommands" :: state
      .copy(definedCommands = state.definedCommands ++ newCommands)
    uuids.foldRight(newState) {
      case (uuid, s) => s"setStart $uuid" :: cmd :: s"setElapsed $uuid" :: s
    }
  }

}
