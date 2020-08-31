package org.saddle.data

import org.saddle.ST
import org.saddle.scalar.Value
import scala.{specialized => spec}

trait Arithmetic {
  data: Data =>
  def op[@spec(Boolean, Int, Double, Float, Long) A: ST](that: Data)(ox: (A, A) => A): Data = data.asInstanceOf[Value[A]].operate[A](that)(ox)

  def comp[@spec(Boolean, Int, Double, Float, Long) A: ST](that: Data)(ox: (A, A) => Boolean): Data = data.asInstanceOf[Value[A]].compare[A](that)(ox)
}
