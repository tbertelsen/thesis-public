package apps

import java.io.File

import geneio.FastaUtils
import sequences.{KmerProfile, DnaSeqBits}
import splot._

import scala.collection.immutable

/**
 * Created by tobber on 8/4/15.
 */
object KmerFrequencies {

  def analyze(dna: DnaSeqBits, k: Int) = {
    val kmers = KmerProfile(k)(dna)
    val combinations = math.round(math.pow(4,k)).toInt
    val places = dna.length - k + 1
    val maxCount = math.min(combinations, places)
    val activeCount = kmers.profile.activeSize
    // Frequencies sorted descending
    val active = kmers.profile.activeIterator
          .map{case (i,v) => (i,math.round(v).toInt)}.toArray.sortBy{case (i,v) =>(-v,i)}
    val mostFrequent = KmerProfile.kmerToString(k)(active(0)._1)
    val frequencies = active.map(_._2)
    val most = frequencies(0)
    val tenAllI = maxCount / 10
    val tenAll = if (tenAllI < frequencies.length)  frequencies(tenAllI) else 0
    val tenActive = frequencies(activeCount / 10)
    val oneAllI = maxCount / 100
    val oneAll = if (oneAllI < frequencies.length)  frequencies(oneAllI) else 0
    val oneActive = frequencies(activeCount / 100)
    KmerFreq(k, maxCount, activeCount, most, tenAll, tenActive, oneAll, oneActive, mostFrequent)
  }

  def main() :Unit = main(Array())

  def main(args: Array[String]) {
    val dataSize = 8;
    run(dataSize)
  }

  def run(dataSize: Int): Unit = {
    val name = "HMP253.%d000".format(dataSize)
    run(name)
  }

  def run(name: String){
    println("Running " + name)
    val genesFile = new File(ProxyTester.geneBucketPath, name + ".fa")
    val outDir = "kmerFrequencies/" + name
    val serialFile = new File(outDir, "data.serial")

    val freqs = if (serialFile.exists()) {
      utils.IO.deserialize[List[KmerFreq]](serialFile)
    } else {
      val genes = FastaUtils.readFastaFile(genesFile)
      val concat = genes.map(_.content.toString()).mkString("")
      val allDna = DnaSeqBits(concat)
      val freqs = (for (k <- 1 to 15) yield {
        println("k = " + k)
        analyze(allDna, k)
      }).toList
      utils.IO.serialize(serialFile, freqs)
      freqs
    }

    freqs.foreach(println)

    val ks = freqs.map(_.k.toDouble)
    val mosts = freqs.map(_.most.toDouble)
    val oneAlls = freqs.map(_.oneAll.toDouble)
    val oneActives = freqs.map(_.oneActive.toDouble)
    val tenAlls = freqs.map(_.tenAll.toDouble)
    val tenActives = freqs.map(_.tenActive.toDouble)

    val firstMost = mosts.head
    val uniform = ks.map(k => math.round(firstMost / math.pow(4, k-1)).toDouble)
    val half = ks.map(k => math.round(firstMost / math.pow(2, k-1)).toDouble)

    utils.IO.stringsToFile(new File(outDir, "data.txt"), freqs)

    def plotFreq(prefix : String, ones : Seq[Double], tens : Seq[Double]) = {
      val plot = linePlot("k", "Occurrences")
      plot += points(ks, mosts, "Most Common")
      plot += points(ks, ones, "1% Frac.")
      plot += points(ks, tens, "10% Frac.")
      plot += points(ks, half, "½ᵏ")
      plot += points(ks, uniform, "¼ᵏ")
      plot.save(outDir + "/" + prefix + "kmerFrequencies.png")
      plot.log().save(outDir + "/" + prefix + "kmerFrequenciesLog.png")
    }

    plotFreq("", oneAlls, tenAlls)
    plotFreq("active", oneActives, tenActives)
  }

  case class KmerFreq(k:Int, maxCount: Int, activeCount: Int, most : Int, tenAll : Int, tenActive : Int, oneAll : Int, oneActive : Int, mostFrequent:String)
    extends Serializable
}
