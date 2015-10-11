package sequences

import testbase.UnitSpec

class CompactIntArrayTest extends UnitSpec {

  "An array" should "throw ArrayIndexOutOfBoundsException" in {
    val array = new CompactIntArray(2, 4);
    intercept[ArrayIndexOutOfBoundsException] {
      array(-1)
    }
    intercept[ArrayIndexOutOfBoundsException] {
      array(-3)
    }
    intercept[ArrayIndexOutOfBoundsException] {
      array(4)
    }
    intercept[ArrayIndexOutOfBoundsException] {
      array(-1) = 1
    }
    intercept[ArrayIndexOutOfBoundsException] {
      array(-3) = 1
    }
    intercept[ArrayIndexOutOfBoundsException] {
      array(4) = 1
    }
    array(0)
    array(3)
    array(0) = 1
    array(3) = 1
  }

  it should "be mutable" in {
    val array = new CompactIntArray(4, 4)
    // NOTE: Indexes are not in order on purpose.
    array(1) = 4
    array(0) = 1
    array(3) = 7
    array(2) = 0
    
    array(1) shouldEqual 4
    array(0) shouldEqual 1
    array(3) shouldEqual 7
    array(2) shouldEqual 0
  }
  
  it should "behave like a sequence" in {
    val array = new CompactIntArray(4, 4)
    array(1) = 4
    array(0) = 1
    array(3) = 7
    array(2) = 0
    
    val double = array map (_*2)
    
    double shouldEqual Seq(2, 8, 0, 14)
    array(1) shouldEqual 4
    array(0) shouldEqual 1
    array(3) shouldEqual 7
    array(2) shouldEqual 0
  }
  
  it should "create an array string" in {
    val array = new CompactIntArray(4, 4)
    array(1) = 4
    array(0) = 1
    array(3) = 7
    array(2) = 0
    
    array.toString shouldEqual "[1, 4, 0, 7]"
  }
  
  it should "not allow negative values" in {
    val array = new CompactIntArray(4, 4)
    array(0) = 0
    intercept [IllegalArgumentException] {
       array(1) = -1;
    }
    intercept [IllegalArgumentException] {
       array(1) = Int.MinValue;
    }
  }
  
    it should "not allow large values" in {
    val array = new CompactIntArray(2, 4)
    array(0) = 0
    array(0) = 3
    intercept [IllegalArgumentException] {
       array(1) = 4;
    }
    intercept [IllegalArgumentException] {
       array(1) = 21348;
    }
  }
}
