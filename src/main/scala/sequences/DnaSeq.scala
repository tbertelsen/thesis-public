package sequences

import sequences.jaligner.JalignerUtils

/**
 * Created by tobber on 8/1/15.
 */
@SerialVersionUID(51021406024445147L)
class DnaSeq(val id: Long, val name: String, val content: DnaSeqBits) extends Serializable {
}

object DnaSeq {

  /**
   * Parse a tuple of (id, fastaString) to a DnaSeq object
   */
  val fromFastaTuple = (tuple: (Long, String)) => {
    try {
      val id = tuple._1
      val string = tuple._2.trim.replace ("\r", "")
      val headStart = if (string (0) == '>') 1 else 0
      val headEnd = string.indexOf ('\n')
      val head = string.substring (headStart, headEnd);
      val content = string.substring (headEnd + 1).replace ("\n", "")
      val name = head.split ("\\s")(0)
      new DnaSeq (id, name, DnaSeqBits (content))
    } catch {
      case e:StringIndexOutOfBoundsException => throw new StringIndexOutOfBoundsException(tuple.toString())
    }
  }

}
