package sequences.jaligner

import breeze.linalg.min
import jaligner.{Alignment, NeedlemanWunschGotoh, NeedlemanWunsch, Sequence}
import jaligner.matrix.Matrix
import sequences.DnaSeq

/**
 * Created by tobber on 12/2/15.
 */
object JalignerUtils {

  val defaultParameters = AlignmentParameters.usearch

  def calcIdentity(seq1: DnaSeq, seq2: DnaSeq) : Double = {
    calcIdentity(seq1, seq2, defaultParameters)
  }

  def getAlignment(seq1: DnaSeq, seq2: DnaSeq, parameters: AlignmentParameters) = {
    NeedlemanWunschGotoh.align(toJseq(seq1), toJseq(seq2), parameters.matrix, parameters.gapOpen, parameters.gapExtend)
  }

  def calcIdentity(seq1: DnaSeq, seq2: DnaSeq, parameters: AlignmentParameters) : Double = {
    try {
      val alignment = getAlignment(seq1, seq2, parameters)
      val minLength = math.min(seq1.content.length, seq2.content.length)
      val minCover = parameters.minCoverFraction * minLength
      val activeLength = math.max(minCover, alignment.getLength - alignment.getTerminalGaps)
      alignment.getIdentity.toDouble / activeLength
    } catch {
      case e: IllegalStateException => {
        e.printStackTrace()
        e.printStackTrace(System.out)
        0.0
      }
    }
  }

  def toJseq(seq :DnaSeq) = {
    new Sequence(seq.content.toString(), seq.name, "ID:" + seq.id, 1)
  }
}

@SerialVersionUID(86172565754842353L)
class AlignmentParameters(
      val matchScore : Float,
      val mismatchScore : Float,
      val gapOpen : Float,
      val gapExtend : Float,
      val minCoverFraction: Double,
      val name : String) extends Serializable {
  if (matchScore < 0.0F || mismatchScore > 0.0F || gapOpen < 0.0F || gapExtend < 0.0F) {
    throw new IllegalArgumentException()
  }

  val scoreMatrix = Array.tabulate[Float](Matrix.SIZE, Matrix.SIZE){
    (i,j) => if (i==j) matchScore else mismatchScore
  }
  val matrix = new Matrix("", scoreMatrix)
}

// Based on http://www.drive5.com/usearch/cdhit_params.html
object AlignmentParameters {
  private def negF(d : Double) = {
    if (d <= 0.0) d.toFloat else - d.toFloat
  }
  private def posF(d : Double) = {
    if (d >= 0.0) d.toFloat else -d.toFloat
  }

  def apply(matchScore : Double, mismatchScore : Double, gapOpen : Double, gapExtend : Double, minCoverFraction: Double, name : String): AlignmentParameters = {
    new AlignmentParameters(posF(matchScore) , negF(mismatchScore), posF(gapOpen) , posF(gapExtend), minCoverFraction, name)
  }
  val cdHit = AlignmentParameters(1.0F, -1.0F, 3.0F, 0.5F, 0.8, "CD-Hit")
  val usearch = AlignmentParameters(1.0F, -2.0F, 10.0F, 0.5F, 0.8, "USearch")
  val blastn = AlignmentParameters(1.0F, -2.0F, 5.0F, 2.0F, 0.8, "BlastN")

  val predefined = List(cdHit, usearch, blastn).map(a => (a.name.toLowerCase, a)).::("cdhit",cdHit).::("uclust",usearch).toMap
  def fromString(name : String) = {
    predefined.get(name.toLowerCase)
  }

}

