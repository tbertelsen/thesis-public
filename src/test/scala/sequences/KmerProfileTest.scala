package sequences

import breeze.linalg.SparseVector
import testbase.UnitSpec

class KmerProfileTest extends UnitSpec {

  "A profile" should "calculate Person's correlation" in {
    testPearson(Array(3, 5, 1, 6, 2), Array(8, 9, 2, 4, 7), 0.289464)
    testPearson(Array(3, 5, 1, 6, 2), Array(6, 11, 1, 11, 4), 0.982449)
    testPearson(Array(0, 1), Array(1, 0), -1)
    testPearson(Array(0, 1), Array(2, 4), 1)
    testPearson(Array(6, 5, 0, 5, 9), Array(10, 14, 10, 6, 10), 0.0)
  }

  def testPearson(arr1: Array[Double], arr2: Array[Double], expected: Double) = {
    val prof1 = new KmerProfile(SparseVector(arr1))
    val prof2 = new KmerProfile(SparseVector(arr2))
    val corr1 = prof1.pearson(prof2)
    val corr2 = prof1.pearson(prof2)
    corr1 shouldEqual corr2 +- 0.00000001
    corr1 shouldEqual expected +- 0.00001
  }

  it should "calculate Cosine similarity correctly" in {
    testCosine(Array(3, 5, 1, 6, 2), Array(8, 9, 2, 4, 7), 0.860377)
    testCosine(Array(3, 5, 1, 6, 2), Array(6, 11, 1, 11, 4), 0.994993)
    testCosine(Array(2, 4), Array(1, 2), 1)
    testCosine(Array(0, 1), Array(2, 0), 0)
    testCosine(Array(6, 5, 0, 5, 9), Array(10, 14, 10, 6, 10), 0.838737)

  }

  def testCosine(arr1: Array[Double], arr2: Array[Double], expected: Double) = {
    val prof1 = new KmerProfile(SparseVector(arr1))
    val prof2 = new KmerProfile(SparseVector(arr2))
    val sim1 = prof1.cosineSimilarity(prof2)
    val sim2 = prof1.cosineSimilarity(prof2)
    sim1 shouldEqual sim2 +- 0.00000001
    sim1 shouldEqual expected +- 0.00001
  }

}
