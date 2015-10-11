package geneio

import java.io.File

import apps.ProxyTester.DnaPack
import sequences.DnaSeq

import scala.io.Source

/**
 * Created by tobber on 13/2/15.
 */
object FastaUtils {

  def readFastaFile(file: File) : List[DnaSeq] ={
    val dataLines = readFastaFileStrings(file)
    dataLines.zipWithIndex.map{x => DnaSeq.fromFastaTuple(x._2, x._1)}.toList
  }

  def readFastaFileStrings(file: File) : Iterator[String] = {
    readDelimited(file, "^>|[\\r]?[\\n]>")
  }

  private def readDelimited(file: File, delimiter : String) : Iterator[String] = {
    val scanner = new java.util.Scanner(file).useDelimiter(delimiter)
    collection.JavaConversions.asScalaIterator(scanner)
  }
}
