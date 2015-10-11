package stats

import breeze.linalg.*

/**
 * Created by tobber on 27/2/15.
 */
class ClassificationStats(val truePos : Int,  val falsePos  : Int,  val trueNeg : Int,  val falseNeg  : Int) {
  def testNeg = trueNeg + falseNeg
  def testPos = truePos + falsePos
  def conditionNeg = trueNeg + falsePos
  def conditionPos = truePos + falseNeg
  def total = testNeg + testPos

  def truePosRate = truePos / conditionPos.toDouble
  def trueNegRate = trueNeg / conditionNeg.toDouble
  def falsePosRate = falsePos / conditionPos.toDouble
  def falseNegRate = falseNeg / conditionNeg.toDouble

  def posPredictValue = truePos / testPos.toDouble
  def negPredictValue = trueNeg / testNeg.toDouble
  def falseDiscoveryRate = falsePos / testPos.toDouble
  def falseOmissionRate = falseNeg / testNeg.toDouble

  def prevalence = conditionPos / total.toDouble
  def accuracy = (truePos + trueNeg)  / total.toDouble

  def posLikelihoodRatio = truePosRate / falsePosRate
  def negLikelihoodRatio = falseNegRate / trueNegRate
  def diagnosticOdds = posLikelihoodRatio / negLikelihoodRatio

  // alternative names
  def sensitivity = truePosRate
  def recall = truePosRate
  def fallOut = falsePosRate
  def specificity = trueNegRate
  def precision = posPredictValue

  // Stats
  def fBeta(beta : Double): Double = {
    (1 + beta * beta) * (precision * recall)/(beta * beta * precision + recall)
  }
  def f1Score = fBeta(1.0)
  def matthews = (truePos * trueNeg - falsePos * falseNeg) / (testPos*testNeg*conditionNeg*conditionPos)
}

object ClassificationStats {

  def apply[T](data : Seq[T])(condition : T => Boolean, test : T => Boolean)  = {
    var truePos = 0
    var falsePos = 0
    var trueNeg = 0
    var falseNeg = 0
    for (d <- data) {
      if (condition(d)) {
        if (test(d)) {
          truePos += 1
        } else {
          falseNeg += 1
        }
      } else {
        if (test(d)) {
          falsePos += 1
        } else {
          trueNeg += 1
        }
      }
    }
    new ClassificationStats(truePos, falsePos, trueNeg, falseNeg)
  }

}