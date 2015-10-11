package sequences

object DnaSeqBits {

  def apply(sequence: CharSequence) = {
    val array = new CompactIntArray(DNA.BITS, sequence.length);
    for (i <- 0 until sequence.length) {
    	array(i) = DNA.toBin(sequence.charAt(i));
    }
    new DnaSeqBits(array)
  }

}

@SerialVersionUID(581265020778811091L)
class DnaSeqBits private (private val sequence: CompactIntArray) extends Serializable {
  def getBin(i: Int) = sequence(i);
  
  def apply(i: Int) = getBin(i) 
  
  val length = sequence.length;
  
  override def toString() = {
    sequence.map(DNA.toChar).addString(new StringBuilder).toString
  }
  
}
