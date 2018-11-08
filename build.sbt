val quickrunPlugin = com.swoval.quickrun.Build.quickrunPlugin

lazy val quickrun = inputKey[Unit]("Run a task quickly")

quickrun := com.swoval.quickrun.Plugin.quickrun.evaluated
