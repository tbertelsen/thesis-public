package collections

import scala.collection.mutable
import scala.collection.mutable.Iterable

/**
 * Created by tobber on 8/1/15.
 */
class MutableMultiSet[A] private(map: mutable.Map[A, Int]) extends Iterable[(A, Int)] {
  override def iterator() = map.iterator

  def add(a: A) : Unit = {
    val prevValue = map.getOrElse(a, 0)
    map.put(a, prevValue + 1)
  }

  def remove(a: A) : Unit = {
    val prevValue = map.getOrElse(a, 0)
    if (prevValue == 1) {
      map.remove(a)
    } else if (prevValue > 1) {
      map.put(a, prevValue - 1)
    }
  }

  def clear(a : A) : Unit = {
    map.remove(a)
  }
}

object MutableMultiSet {
  def apply[A]() = {
    new MutableMultiSet[A](mutable.Map.empty)
  }
}
