package breeze

import breeze.linalg._
import breeze.stats._
import scala.math.sqrt



/**
 * Created by tobber on 8/1/15.
 */
object Utils {

  /**
   * Effecient for sparse vectors. Scales in O(activeSize)
   *
   */
  def pearson(a: SparseVector[Double], b: SparseVector[Double]): Double = {
    if (a.length != b.length)
      throw new IllegalArgumentException("Vectors not of the same length.")

    val n = a.length

    val dot = a.dot(b)
    val adot = a.dot(a)
    val bdot = b.dot(b)
    val amean = mean(a)
    val bmean = mean(b)

    // See Wikipedia http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient#For_a_sample
    (dot - n * amean * bmean ) / ( sqrt(adot - n * amean * amean)  * sqrt(bdot - n * bmean * bmean) )
  }

  def normalize(vector: SparseVector[Double]) = {
    // More effecient than breeze.linalg.normalize(profile) (in 0.10)
    // Breeze 0.11.1 should be effective enught
    val norm = breeze.linalg.norm(vector)
    val poss = vector.activeIterator.map{case (pos,v) => pos}
    val values = vector.activeIterator.map{case (pos,v) => v / norm}
    new SparseVector[Double](poss.toArray, values.toArray, vector.length)
  }
}
