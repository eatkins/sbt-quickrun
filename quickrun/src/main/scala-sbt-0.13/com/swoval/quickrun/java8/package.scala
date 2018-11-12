package com.swoval.quickrun

package object java8 {
  implicit class FunctionOps[T, R](f: T => R) extends java.util.function.Function[T, R] {
    override def apply(t: T): R = f(t)
  }
}