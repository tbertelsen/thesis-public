package sequences

@SerialVersionUID(488413459350845191L)
class CompactIntArray(val elemBits: Int, val length: Int) extends IndexedSeq[Int] with Serializable {
  class caIterator extends Iterator[Int] {
    private var index = 0;
    def hasNext() = index < CompactIntArray.this.length
    def next = {
      index += 1;
      get(index - 1);
    }
  }

  if (32 % elemBits != 0) {
    throw new IllegalArgumentException("elemBits must be a divisor of 32:" + elemBits)
  }
  private val elemPerInt = 32 / elemBits;
  private val elemMask = ~(-1 << elemBits)

  val arr = Array.ofDim[Int](length / elemPerInt + 1)

  private def checkIndex(index: Int) = {
    if (index >= length || index < 0) {
      throw new ArrayIndexOutOfBoundsException("Index " + index + " not within  0-" + length)
    }
  }

  private def checkValue(value: Int) = {
    if ((value & ~elemMask) != 0) {
      throw new IllegalArgumentException(value.toBinaryString + " is longer than " + elemBits + " bits long!")
    }
  }

  def get(index: Int) = {
    checkIndex(index)
    val location = arr(index / elemPerInt)
    (location >> (elemBits * (index % elemPerInt))) & elemMask
  }

  def set(index: Int, value: Int) = {
    checkIndex(index)
    checkValue(value)
    val location = arr(index / elemPerInt)
    val offset = (elemBits * (index % elemPerInt));
    arr(index / elemPerInt) =
      // Clear the location (set to 0's)
      (location & ~(elemMask << offset)) |
        // Insert the value (by OR-ing on the prev. line)
        (value << offset)
  }

  override def apply(i: Int) = get(i)

  def update(i: Int, value: Int) = set(i, value)

  override def iterator() = new caIterator()

  override def toString() = {
    var sb = new StringBuilder();
    sb + '['
    addString(sb, ", ")
    sb + ']'
    sb.toString
  }
}
