package utils

/**
 * Created by tobber on 19/3/15.
 */
object Equations {
  val lnOf2 = scala.math.log(2) // natural log of 2
  def log2(x: Double): Double = scala.math.log(x) / lnOf2

  def elementInformationScore(value: Double, minValue: Double, alpha: Double): Double = {
    if (value > minValue) {
      math.pow ((value - minValue) / (1.0 - minValue), alpha)
    } else {
      0
    }
  }

  def information(elements: Seq[Double], minValue: Double, alpha: Double): Double = {
    val score = elements.map(elementInformationScore(_, minValue, alpha)).sum
    log2(1.0 + score)
  }
}
