package org.saddle.data

import org.saddle._
import org.saddle.scalar.{Data, Value}

import scala.{specialized => spec}

/**
  * Meta-typeclass for Data and Value
  */

trait Arithmetic {
  data: Data =>
  def op[@spec(Boolean, Int, Double, Float, Long) A: ST](that: Data)(ox: (A, A) => A): Data = data.asInstanceOf[Value[A]].operate[A](that)(ox)

  def comp[@spec(Boolean, Int, Double, Float, Long) A: ST](that: Data)(ox: (A, A) => Boolean): Data = data.asInstanceOf[Value[A]].compare[A](that)(ox)
}

trait MetaValue[@spec(Boolean, Int, Double, Float, Long) +T] {
  value: Value[T] =>

  def operate[@spec(Boolean, Int, Double, Float, Long) A: ST](
      that: Data
  )(ox: (T, T) => A): Data =
    that match {
      case v: Value[T] => Value(ox(el, v.el))
      case _           => ???
    }

  def compare[@spec(Boolean, Int, Double, Float, Long) A: ST](
      that: Data
  )(ox: (T, T) => Boolean): Data =
    that match {
      case v: Value[T] => Value(ox(el, v.el))
      case _           => ???
    }

}
