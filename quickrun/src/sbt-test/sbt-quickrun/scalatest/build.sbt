val test = (project in file(".")).settings(
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  com.swoval.quickrun.scripted.Build.extraSettings
)