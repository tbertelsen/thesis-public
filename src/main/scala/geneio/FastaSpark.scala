package geneio

import java.io.File
import java.net.URI

import apps.SimpleSpark
import org.apache.spark.SparkContext
import sequences.DnaSeq

/**
 * Created by Tobber on 8/1/15.
 */
object FastaSpark {

  def readFastaFile(sc : SparkContext, uri : URI) = {
    SimpleSpark.delimitedFile(sc, uri, ">") filter (_._2.trim.length > 0) map DnaSeq.fromFastaTuple
  }

}
