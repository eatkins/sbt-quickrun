val akkaTest = (project in file(".")).settings(
  name := "akka-test",
  scalaVersion := "2.12.7",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.16",
  com.swoval.quickrun.QuickrunPlugin.allSettings
)
