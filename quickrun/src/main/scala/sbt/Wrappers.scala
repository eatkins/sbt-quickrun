package sbt

object Wrappers {
  def inAggregates(ref: ProjectReference): ScopeFilter.ProjectFilter =
    ScopeFilter.Make.inAggregates(ref)
  def inDeps(ref: ProjectReference): ScopeFilter.ProjectFilter =
    ScopeFilter.Make.inDependencies(ref)
}
