name := "akka-test"

scalaVersion := "2.12.7"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.16"

val quickrunTest = inputKey[Unit]("Run the quick run test")

quickrunTest := Def.inputTaskDyn {
  val output = Def.spaceDelimited().parsed.head
  val file = " " + (target.value / output)
  Def.task(quickrun.toTask(file).value)
}.evaluated
