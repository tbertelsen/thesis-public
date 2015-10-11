package sequences

import breeze.linalg._
import collections.MutableMultiSet

import scala.language.postfixOps
import scala.math.{log, min}

object KmerProfile {

  // We cannot use 32 bits, since that would create negative array indexes.
  val MAX_K = 31 / DNA.BITS;

  private def kmerProfile(k: Int, sequence: DnaSeqBits) = {
    if (k < 1 || k > MAX_K) {
      throw new IllegalArgumentException("K : " + k + " is not within 1-" + MAX_K)
    }
    val bitmask = ~(-1 << (k * DNA.BITS))
    val profile = MutableMultiSet[Int]();

    var kmer = 0;
    // Initialize the first kmer
    for (i <- 0 until k) {
      kmer = (kmer << DNA.BITS) & bitmask;
      kmer = kmer | sequence.getBin(i);
    }
    // Find all other kmers 
    for (i <- k until sequence.length) {
      kmer = (kmer << DNA.BITS) & bitmask;
      kmer = kmer | sequence.getBin(i);
      profile.add(kmer);
    }
    // Create a Vector
    val kmerCombinations = 1 << (k * DNA.BITS)
    val array = profile.toArray.sorted;
    val (poss, vals) = array.unzip
    new SparseVector[Double](poss.toArray, vals.toArray.map(_.toDouble), poss.length, kmerCombinations)
  }

  def apply(k: Int)(sequence: DnaSeqBits) = {
    new KmerProfile(kmerProfile(k, sequence))
  }

  def kmerToString(k : Int)(kmer : Int) = {
    val mask = 3 // 11 in binary
    val chars = for (i <- 1 to k) yield {
      val shift = DNA.BITS * (k - i)
      val charBits = (kmer >> shift) & mask
      DNA.toChar(charBits)
    }
    chars.mkString("")
  }
}

@SerialVersionUID(419504093113690937L)
class KmerProfileNormalized private[sequences] (val profile: SparseVector[Double]) extends Serializable {
  def apply(i: Int) = profile(i)

  def dotProduct(other: KmerProfileNormalized) : Double = {
    profile.dot(other.profile)
  }

  def cosineSimilarity(other: KmerProfileNormalized) = {
    dotProduct(other)
  }

  def activeIterator() = profile.activeIterator
}

@SerialVersionUID(766514920490888591L)
class KmerProfile private[sequences] (val profile: SparseVector[Double]) extends Serializable {

  // Guarantee that cross product will not overflow.
  // sqrt(Int.MaxValue) = 46340.95
  if (profile.activeValuesIterator.exists(_ > 46340)) {
    throw new IllegalArgumentException("The profile will suffer from overflowing.");
  }

  def apply(i: Int) = profile(i)
  val length = profile.length
  lazy val norm : Double = breeze.linalg.norm(profile)
  lazy val count = breeze.linalg.norm(profile, 1)
  lazy val normalize = {
    new KmerProfileNormalized(breeze.Utils.normalize(profile))
  }

  def dotProduct(other: KmerProfile) : Double = {
    if (other.length != length) {
      throw new IllegalArgumentException("Profiles not of equal lengths %d != %d.".format(length, other.length))
    }
    profile.dot(other.profile)
  }

  private def commonCount(other: KmerProfile) = {
    val len1 = profile.activeSize
    val len2 = other.profile.activeSize

    var offset1, offset2 = 0
    var count = 0.0
    while (offset1 < len1 && offset2 < len2) {
      val indexCompare = profile.indexAt(offset1) - other.profile.indexAt(offset2)
      if (indexCompare < 0) {
        offset1 += 1
      } else if (indexCompare > 0) {
        offset2 += 1
      } else {
        count += min(profile.valueAt(offset1), other.profile.valueAt(offset2))
        offset1 += 1
        offset2 += 1
      }
    }

    count
  }

  def commonCountFrac(other: KmerProfile) = {
    // Edgar 2004, Eq 3
    commonCount(other) / count
  }

  def edgarKmerDistance(other: KmerProfile) = {
    // Edgar 2004, Eq 4
    log(0.1 + commonCountFrac(other))
  }

  def pearson(other: KmerProfile) = {
    breeze.Utils.pearson(profile, other.profile)
  }

  def cosineSimilarity(other: KmerProfile) = {
    val dotP = dotProduct(other);
    dotP / (norm * other.norm)
  }
}
