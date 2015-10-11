package utils

import scala.math.Ordering.Implicits._
import scala.reflect.runtime.universe._

/**
 * Created by tobber on 12/3/15.
 */
trait TupleRef extends scala.Product {

  protected def asTuple() : Product

  override def canEqual(other: Any) = {
    other.isInstanceOf[TupleRef]
  }

  override def equals(that: Any) = {
    canEqual(that) && asTuple().equals(that.asInstanceOf[TupleRef].asTuple())
  }

//  override def compare(that : C) = {
//    compareT(this.asTuple(), that.asTuple())
//  }
//
//  def compareT(thisT : T, thatT : T)(implicit ord : Ordering[T]) = {
//    ord.compare(thisT, thatT)
//  }

  override def hashCode() = asTuple().hashCode()

  override def toString() = asTuple().toString

  override def productElement(n: Int) = asTuple().productElement(n)

  override def productArity = asTuple().productArity

  override def productIterator = asTuple().productIterator

  override def productPrefix = asTuple().productPrefix
}
