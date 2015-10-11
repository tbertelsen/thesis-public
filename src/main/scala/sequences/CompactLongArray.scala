package sequences

@SerialVersionUID(157088410979846908L)
class CompactLongArray(val elemBits: Int, val length: Int) extends IndexedSeq[Long] with Serializable {
  class caIterator extends Iterator[Long] {
    private var index = 0;
    def hasNext() = index < CompactLongArray.this.length
    def next = {
      index += 1;
      get(index - 1);
    }
  }

  if (64 % elemBits != 0) {
    throw new IllegalArgumentException("elemBits must be a divisor of 64:" + elemBits)
  }
  private val elemPerLong = 64 / elemBits;
  private val elemMask = ~(-1L << elemBits)

  val arr = Array.ofDim[Long](length / elemPerLong + 1)

  private def checkIndex(index: Int) = {
    if (index >= length || index < 0) {
      throw new ArrayIndexOutOfBoundsException("Index " + index + " not within  0-" + length)
    }
  }

  private def checkValue(value: Long) = {
    if ((value & ~elemMask) != 0) {
      throw new IllegalArgumentException(value.toBinaryString + " is longer than " + elemBits + " bits long!")
    }
  }

  def get(index: Int) = {
    checkIndex(index)
    val location = arr(index / elemPerLong)
    (location >> (elemBits * (index % elemPerLong))) & elemMask
  }

  def set(index: Int, value: Long) = {
    checkIndex(index)
    checkValue(value)
    val location = arr(index / elemPerLong)
    val offset = (elemBits * (index % elemPerLong));
    arr(index / elemPerLong) =
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
