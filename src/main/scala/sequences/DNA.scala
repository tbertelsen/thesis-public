package sequences

object DNA {
  val BITS = 2;
  // Must be at most 2^BITS long:
  private val letters =  Array('A', 'C', 'G', 'T')

  val toBin = (c : Char) => {
    var i = letters.indexOf(c);
    if (i < 0) {
      throw new IllegalArgumentException(c.toString)
    }
    i
  }
  
  val toChar = (i : Int) => {
    letters(i)
  }
}

class DNA private ()
