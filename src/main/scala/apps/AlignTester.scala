package apps

import java.io.File

import geneio.{FastaUtils, AlignmentCache}
import jaligner.Alignment
import sequences.jaligner.{AlignmentParameters, JalignerUtils}

/**
 * Created by tobber on 13/2/15.
 */
object AlignTester {


  def compareAlignments(fastaFile: File, cacheFile : File) = {
    val cache = AlignmentCache(cacheFile)
    val dnaSeqs = FastaUtils.readFastaFile(fastaFile)
    val dnaMap = dnaSeqs.map(dna => (dna.name, dna)).toMap

    val names = List("usearch", "cdhit", "blastn")
    val params = List(AlignmentParameters.usearch, AlignmentParameters.cdHit, AlignmentParameters.blastn)
    print("\t\tggsearch\t")
    println(names.map(x => Seq(x,x,x).mkString("\t")).mkString("\t"))
    print("len1\tlen2\talignment\t")
    println(names.map(x => Seq("identity",x,x).mkString("\t")).mkString("\t"))
    for (entry <- cache; if entry._2 > 70.0 && entry._2 < 100.0) {
      val (name1, name2) = entry._1
      try {
        val dna1 = dnaMap.get(name1).get
        val dna2 = dnaMap.get(name2).get
        // identities:
        val calculatedSimilarities = params.map{p => JalignerUtils.calcIdentity(dna1, dna2, p)}
        val similarities = (entry._2 / 100.0) :: calculatedSimilarities
        println(similarities.mkString("\t"))
      } catch {
        case _ : Exception => println("Could not find %s or %s".format(name1, name2))
      }

    }
  }

  def compareAlignments(name : String) : Unit = {
    val genes = new File(ProxyTester.geneBucketPath, name + ".fa")
    val ggAlign = new File(ProxyTester.geneBucketPath, name + ".ggalign")
    compareAlignments(genes, ggAlign)
  }

  def printAlign(align : Alignment) = {
    println(new String(align.getSequence1))
    println(new String(align.getMarkupLine))
    println(new String(align.getSequence2))
  }

}
