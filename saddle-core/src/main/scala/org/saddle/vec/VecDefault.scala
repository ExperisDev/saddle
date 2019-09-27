/**
  * Copyright (c) 2013 Saddle Development Team
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package org.saddle.vec

import scala.{specialized => spec}
import org.saddle.util.Concat.Promoter
import org.saddle.scalar._
import org.saddle.ops.NumericOps
import org.saddle._
import org.saddle.index.Slice
import org.saddle.index.IndexIntRange
import java.io.OutputStream

class VecDefault[@spec(Boolean, Int, Long, Double) T](
    values: Array[T],
    val scalarTag: ST[T]
) extends NumericOps[Vec[T]]
    with Vec[T] { self =>
  implicit private[this] def st: ST[T] = scalarTag

  /**
    * Set to true when the vec is shifted over the backing array
    * false iff the backing array is a contiguous sequence of the elements of this Vec
    * false iff 0 until length map raw toArray structurally equals the backing array
    */
  override def needsCopy: Boolean = false

  /**
    * The number of elements in the container                                                  F
    */
  def length = values.length

  /**
    * Access an unboxed element of a Vec[A] at a single location
    * @param loc offset into Vec
    */
  def raw(loc: Int): T = values(loc)

  /** Returns an array containing the elements of this Vec in contiguous order
    *
    * May or may not return the backing array, therefore mutations to the returned array
    * may or may not are visible to this Vec
    *
    * If `needsCopy` is false then it returns the backing array
    * If `needsCopy` is true then the backing array is not contiguous
    */
  def toArray: Array[T] = {
    // need to check if we're a view on an array
    if (!needsCopy)
      values
    else {
      val buf = new Array[T](length)
      var i = 0
      while (i < length) {
        buf(i) = raw(i)
        i += 1
      }
      buf
    }
  }

  /**
    * Returns a Vec whose backing array has been copied
    */
  def copy: Vec[T] = Vec(this.contents)

  /**
    * Return copy of backing array
    */
  def contents: Array[T] = if (needsCopy) toArray else toArray.clone()

  /**
    * Equivalent to slicing operation; e.g.
    *
    * {{{
    *   val v = Vec(1,2,3)
    *   v.take(0,1) == v(0,1)
    * }}}
    *
    * @param locs Location of elements to take
    */
  def take(locs: Array[Int]): Vec[T] =
    Vec(array.take[T](toArray, locs, scalarTag.missing))

  /**
    * The complement of the take operation; slice out
    * elements NOT specified in list.
    *
    * @param locs Location of elements not to take
    */
  def without(locs: Array[Int]): Vec[T] = Vec(array.remove(toArray, locs))

  /**
    * Drop the elements of the Vec which are NA
    */
  def dropNA: Vec[T] = filter(_ => true)

  /**
    * Return true if there is an NA value in the Vec
    */
  def hasNA: Boolean = VecImpl.findOneNA(this)

  /**
    * Additive inverse of Vec with numeric elements
    *
    */
  def unary_-()(implicit num: NUM[T]): Vec[T] = map(num.negate)

  /**
    * Concatenate two Vec instances together, where there exists some way to
    * join the type of elements. For instance, Vec[Double] concat Vec[Int]
    * will promote Int to Double as a result of the implicit existence of an
    * instance of Promoter[Double, Int, Double]
    *
    * @param v  Vec[B] to concat
    * @param wd Implicit evidence of Promoter[A, B, C]
    * @param mc Implicit evidence of ST[C]
    * @tparam B type of other Vec elements
    * @tparam C type of resulting Vec elements
    */
  def concat[
      @spec(Boolean, Int, Long, Double) B,
      @spec(Boolean, Int, Long, Double) C
  ](v: Vec[B])(implicit wd: Promoter[T, B, C], mc: ST[C]): Vec[C] =
    Vec(util.Concat.append[T, B, C](toArray, v.toArray))

  /**
    * Left fold over the elements of the Vec, as in scala collections library
    */
  def foldLeft[@spec(Boolean, Int, Long, Double) B: ST](
      init: B
  )(f: (B, T) => B): B =
    VecImpl.foldLeft(this)(init)(f)

  /**
    * Left fold that folds only while the test condition holds true. As soon as the condition function yields
    * false, the fold returns.
    *
    * @param cond Function whose signature is the same as the fold function, except that it evaluates to Boolean
    */
  def foldLeftWhile[@spec(Boolean, Int, Long, Double) B: ST](
      init: B
  )(f: (B, T) => B)(cond: (B, T) => Boolean): B =
    VecImpl.foldLeftWhile(this)(init)(f)(cond)

  /**
    * Filtered left fold over the elements of the Vec, as in scala collections library
    */
  def filterFoldLeft[@spec(Boolean, Int, Long, Double) B: ST](
      pred: T => Boolean
  )(init: B)(f: (B, T) => B): B =
    VecImpl.filterFoldLeft(this)(pred)(init)(f)

  /**
    * Filtered left scan over elements of the Vec, as in scala collections library
    */
  def filterScanLeft[@spec(Boolean, Int, Long, Double) B: ST](
      pred: T => Boolean
  )(init: B)(f: (B, T) => B): Vec[B] =
    VecImpl.filterScanLeft(this)(pred)(init)(f)

  /**
    * Produce a Vec whose entries are the result of executing a function on a sliding window of the
    * data.
    * @param winSz Window size
    * @param f Function Vec[A] => B to operate on sliding window
    * @tparam B Result type of function
    */
  def rolling[@spec(Boolean, Int, Long, Double) B: ST](
      winSz: Int,
      f: Vec[T] => B
  ): Vec[B] =
    VecImpl.rolling(this)(winSz, f)

  /**
    * Map a function over the elements of the Vec, as in scala collections library
    */
  def map[@spec(Boolean, Int, Long, Double) B: ST](f: T => B): Vec[B] =
    VecImpl.map(this)(f)

  /**
    * Maps a function over elements of the Vec and flattens the result.
    */
  def flatMap[@spec(Boolean, Int, Long, Double) B: ST](f: T => Vec[B]): Vec[B] =
    VecImpl.flatMap(this)(f)

  /**
    * Left scan over the elements of the Vec, as in scala collections library
    */
  def scanLeft[@spec(Boolean, Int, Long, Double) B: ST](init: B)(
      f: (B, T) => B
  ): Vec[B] = VecImpl.scanLeft(this)(init)(f)

  /**
    * Zips Vec with another Vec and applies a function to the paired elements. If either of the pair is NA, the
    * result is forced to NA.
    * @param other Vec[B]
    * @param f Function (A, B) => C
    * @tparam B Parameter of other Vec
    * @tparam C Result of function
    */
  def zipMap[
      @spec(Int, Long, Double) B: ST,
      @spec(Boolean, Int, Long, Double) C: ST
  ](other: Vec[B])(f: (T, B) => C): Vec[C] =
    VecImpl.zipMap(this, other)(f)

  /**
    * Creates a view into original vector from an offset up to, but excluding,
    * another offset. Data is not copied.
    *
    * @param from Beginning offset
    * @param until One past ending offset
    * @param stride Increment within slice
    */
  def slice(from: Int, until: Int, stride: Int = 1) = {
    val b = math.max(from, 0)
    val e = math.min(until, self.length)

    if (e <= b) Vec.empty
    else
      new VecDefault(values, scalarTag) {
        private val ub = math.min(self.length, e)

        override def length = math.ceil((ub - b) / stride.toDouble).toInt

        override def raw(i: Int): T = {
          val loc = b + i * stride
          if (loc >= ub)
            throw new ArrayIndexOutOfBoundsException(
              "Cannot access location %d >= length %d".format(loc, ub)
            )
          self.raw(loc)
        }

        override def needsCopy = true
      }
  }

  /**
    * Creates a view into original Vec, but shifted so that n
    * values at the beginning or end of the Vec are NA's. Data
    * is not copied.
    * ex. shift(1)  : [1 2 3 4] => [NA 1 2 3]
    *     shift(-1) : [1 2 3 4] => [2 3 4 NA]
    *
    * @param n Number of offsets to shift
    */
  def shift(n: Int) = {
    val m = math.min(n, self.length)
    val b = -m
    val e = self.length - m

    new VecDefault(values, scalarTag) {
      override def length = self.length

      override def raw(i: Int): T = {
        val loc = b + i
        if (loc >= e || loc < b)
          throw new ArrayIndexOutOfBoundsException(
            "Cannot access location %d (vec length %d)".format(i, self.length)
          )
        else if (loc >= self.length || loc < 0)
          scalarTag.missing
        else
          self.raw(loc)
      }

      override def needsCopy = true
    }
  }

  /**
    * Access a boxed element of a Vec[A] at a single location
    * @param loc offset into Vec
    */
  def at(loc: Int): Scalar[T] = {
    Scalar(raw(loc))(scalarTag)
  }

  /**
    * Slice a Vec at a sequence of locations, e.g.
    *
    * val v = Vec(1,2,3,4,5)
    * v(1,3) == Vec(2,4)
    *
    * @param locs locations at which to slice
    */
  def apply(locs: Int*): Vec[T] = take(locs.toArray)

  /**
    * Slice a Vec at a sequence of locations, e.g.
    *
    * val v = Vec(1,2,3,4,5)
    * v(Array(1,3)) == Vec(2,4)
    *
    * @param locs locations at which to slice
    */
  def apply(locs: Array[Int]): Vec[T] = take(locs)

  /**
    * Slice a Vec at a bound of locations, e.g.
    *
    * val v = Vec(1,2,3,4,5)
    * v(1->3) == Vec(2,3,4)
    *
    * @param rng evaluates to IRange
    */
  def apply(rng: Slice[Int]): Vec[T] = {
    val idx = new IndexIntRange(length)
    val pair = rng(idx)
    slice(pair._1, pair._2)
  }

  /**
    * Access the first element of a Vec[A], or NA if length is zero
    */
  def first: Scalar[T] = {
    if (length > 0) Scalar(raw(0))(scalarTag) else NA
  }

  /**
    * Access the last element of a Vec[A], or NA if length is zero
    */
  def last: Scalar[T] = {
    if (length > 0) Scalar(raw(length - 1))(scalarTag) else NA
  }

  /**
    * Return first n elements
    * @param n Number of elements to access
    */
  def head(n: Int): Vec[T] = slice(0, n)

  /**
    * Return last n elements
    * @param n Number of elements to access
    */
  def tail(n: Int): Vec[T] = slice(length - n, length)

  /**
    * True if and only if number of elements is zero
    */
  def isEmpty: Boolean = length == 0

  /**
    * Returns Vec whose locations corresponding to true entries in the
    * boolean input mask vector are set to NA
    *
    * @param m Mask vector of Vec[Boolean]
    */
  def mask(m: Vec[Boolean]): Vec[T] =
    VecImpl.mask(this, m, scalarTag.missing)(scalarTag)

  /**
    * Returns Vec whose locations are NA where the result of the
    * provided function evaluates to true
    *
    * @param f A function taking an element and returning a Boolean
    */
  def mask(f: T => Boolean): Vec[T] =
    VecImpl.mask(this, f, scalarTag.missing)(scalarTag)

  /**
    * Execute a (side-effecting) operation on each (non-NA) element in the vec
    * @param op operation to execute
    */
  def foreach(op: T => Unit): Unit = { VecImpl.foreach(this)(op)(scalarTag) }

  /**
    * Execute a (side-effecting) operation on each (non-NA) element in vec which satisfies
    * some predicate.
    * @param pred Function A => Boolean
    * @param op Side-effecting function
    */
  def forall(pred: T => Boolean)(op: T => Unit): Unit =
    VecImpl.forall(this)(pred)(op)(scalarTag)

  /**
    * Return Vec of integer locations (offsets) which satisfy some predicate
    * @param pred Predicate function from A => Boolean
    */
  def find(pred: T => Boolean): Vec[Int] = VecImpl.find(this)(pred)(scalarTag)

  /**
    * Return first integer location which satisfies some predicate, or -1 if there is none
    * @param pred Predicate function from A => Boolean
    */
  def findOne(pred: T => Boolean): Int = VecImpl.findOne(this)(pred)(scalarTag)

  /**
    * Return true if there exists some element of the Vec which satisfies the predicate function
    * @param pred Predicate function from A => Boolean
    */
  def exists(pred: T => Boolean): Boolean = findOne(pred) != -1

  /**
    * Return Vec whose elements satisfy a predicate function
    * @param pred Predicate function from A => Boolean
    */
  def filter(pred: T => Boolean): Vec[T] = VecImpl.filter(this)(pred)(scalarTag)

  /**
    * Return vec whose offets satisfy a predicate function
    * @param pred Predicate function from Int => Boolean
    */
  def filterAt(pred: Int => Boolean): Vec[T] =
    VecImpl.filterAt(this)(pred)(scalarTag)

  /**
    * Return Vec whose elements are selected via a Vec of booleans (where that Vec holds the value true)
    * @param pred Predicate vector: Vec[Boolean]
    */
  def where(pred: Vec[Boolean]): Vec[T] =
    VecImpl.where(this)(pred.toArray)(scalarTag)

  /**
    * Yield a Vec whose elements have been sorted (in ascending order)
    * @param ev evidence of Ordering[A]
    */
  def sorted(implicit ev: ORD[T], st: ST[T]) = take(array.argsort(toArray))

  /**
    * Yield a Vec whose elements have been reversed from their original order
    */
  def reversed: Vec[T] =
    Vec(array.reverse(toArray))

  /**
    * Creates a view into original vector from an offset up to, and including,
    * another offset. Data is not copied.
    *
    * @param from Beginning offset
    * @param to Ending offset
    * @param stride Increment within slice
    */
  def sliceBy(from: Int, to: Int, stride: Int = 1): Vec[T] =
    slice(from, to + stride, stride)

  /**
    * Split Vec into two Vecs at position i
    * @param i Position at which to split Vec
    */
  def splitAt(i: Int): (Vec[T], Vec[T]) = (slice(0, i), slice(i, length))

  /**
    * Fills NA values in vector with result of a function which acts on the index of
    * the particular NA value found
    *
    * @param f A function from Int => A; yields value for NA value at ith position
    */
  def fillNA(f: Int => T): Vec[T] = VecImpl.vecfillNA(this)(f)(scalarTag)

  /**
    * Converts Vec to an indexed sequence (default implementation is immutable.Vector)
    *
    */
  def toSeq: IndexedSeq[T] = toArray.toIndexedSeq

  /** Sums up the elements of a numeric Vec
    *
    * NOTE: scalac only specialized correctly if using the method in VecImpl
    * referring to this.filterFoldLeft boxes
    */
  def sum(implicit na: NUM[T], st: ST[T]): T =
    VecImpl.filterFoldLeft(this)(st.notMissing)(st.zero)(
      (a, b) => na.plus(a, b)
    )

  /** Counts the number of non-NA elements
    */
  def count: Int =
    VecImpl.filterFoldLeft(this)(scalarTag.notMissing)(0)((a, _) => a + 1)

  /** Counts the number of non-NA elements satisfying the predicate
    */
  def countif(test: T => Boolean): Int =
    VecImpl.filterFoldLeft(this)(t => scalarTag.notMissing(t) && test(t))(0)(
      (a, _) => a + 1
    )

  /** Counts the number of elements which equal `a`
    */
  def countif(a: T): Int =
    VecImpl.filterFoldLeft(this)(t => t == a)(0)((a, _) => a + 1)

  private[saddle] def toDoubleArray(implicit na: NUM[T]): Array[Double] = {
    val arr = toArray
    val buf = new Array[Double](arr.length)
    var i = 0
    while (i < arr.length) {
      buf(i) = scalarTag.toDouble(arr(i))
      i += 1
    }
    buf
  }

  /** Default hashcode is simple rolling prime multiplication of sums of hashcodes for all values. */
  override def hashCode(): Int = foldLeft(1)(_ * 31 + _.hashCode())

  /**
    * Default equality does an iterative, element-wise equality check of all values.
    *
    */
  override def equals(o: Any): Boolean = o match {
    case rv: Vec[_] =>
      (this eq rv) || (this.length == rv.length) && {
        var i = 0
        var eq = true
        while (eq && i < this.length) {
          eq &&= (raw(i) == rv.raw(i) || this.scalarTag
            .isMissing(raw(i)) && rv.scalarTag.isMissing(rv.raw(i)))
          i += 1
        }
        eq
      }
    case _ => false
  }

  /**
    * Creates a string representation of Vec
    * @param len Max number of elements to include
    */
  def stringify(len: Int = 10): String = {
    val half = len / 2

    val buf = new StringBuilder()

    implicit val st = scalarTag

    val maxf = (a: Int, b: String) => math.max(a, b.length)

    if (length == 0)
      buf append "Empty Vec"
    else {
      buf.append("[%d x 1]\n" format (length))
      val vlen = { head(half) concat tail(half) }
        .map(scalarTag.show(_))
        .foldLeft(0)(maxf)

      def createRow(r: Int): String =
        ("%" + { if (vlen > 0) vlen else 1 } + "s\n")
          .format(scalarTag.show(raw(r)))
      buf append util.buildStr(len, length, createRow, " ... \n")
    }

    buf.toString()
  }

  /**
    * Pretty-printer for Vec, which simply outputs the result of stringify.
    * @param len Number of elements to display
    */
  def print(len: Int = 10, stream: OutputStream = System.out) {
    stream.write(stringify(len).getBytes)
  }

  override def toString = stringify()

  /**
    * Rounds elements in the vec (which must be numeric) to
    * a significance level
    *
    * @param sig Significance level to round to (e.g., 2 decimal places)
    */
  def roundTo(sig: Int = 2)(implicit ev: NUM[T]): Vec[Double] = {
    val pwr = math.pow(10, sig)
    val rounder = (x: T) => math.round(scalarTag.toDouble(x) * pwr) / pwr
    map(rounder)
  }

  /** Returns a new Vec with the value at `offset` set to `value
    *
    * Copies before mutating.
    */
  def updated(offset: Int, value: T): Vec[T] = {
    val ar = copy.toArray
    ar(offset) = value
    Vec(ar)
  }

  /** Returns a new Vec with the value at `offset` set to `value
    *
    * Copies before mutating.
    * Ignores invalid offsets in the array
    */
  def updated(offset: Array[Int], value: T): Vec[T] = {
    val ar = copy.toArray
    var i = 0
    val n = offset.size
    while (i < n) {
      val j = offset(i)
      if (j < length) {
        ar(j) = value
      }
      i += 1
    }
    Vec(ar)
  }
}
