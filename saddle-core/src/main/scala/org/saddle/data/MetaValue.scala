package org.saddle.data

import org.saddle.ST
import org.saddle.scalar.Value
import scala.{specialized => spec}

trait MetaValue [@spec(Boolean, Int, Double, Float, Long) +T] {
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