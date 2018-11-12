package sbt

object NamedCommand {
  def unapply(c: Command): Option[SimpleCommand] = c match {
    case s: SimpleCommand => Some(s)
    case _                => None
  }
}
