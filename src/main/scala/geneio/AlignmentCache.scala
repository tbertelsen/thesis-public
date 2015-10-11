package geneio

import java.io.File

import scala.collection.immutable.Map
import scala.io.Source

object AlignmentCache {
  
  def apply(file : File) = {
    def splitLine(line: String) = {
      val s = line.split('\t')
      ((s(0), s(1)), s(2).toDouble)
    }
    val lines = Source.fromFile(file).getLines()
    val entries = lines filter {!_.startsWith("#")} map splitLine
    new AlignmentCache(entries.toMap)
  }
  
}

class AlignmentCache private (map : Map[Tuple2[String, String], Double]) extends Iterable[((String, String), Double)] {
	def get(id1: String, id2 :String) = {
	  map.get((id1, id2))
	}
  def iterator() = {
    map.iterator
  }
}
