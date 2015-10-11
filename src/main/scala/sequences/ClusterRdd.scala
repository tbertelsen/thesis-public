package sequences

import java.io.File
import java.net.URI

import apps.{SimpleSpark, Assigned, Center}
import apps.SparkClustering.{ClusterGraph, PartiallyAssignedGraph}
import geneio.{FastaSpark, UClustLine}
import org.apache.spark.{rdd, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import sequences.jaligner.{JalignerUtils, AlignmentParameters}
import spire.std.string
import utils.Equations.information


/**
 * Created by tobber on 19/3/15.
 */
class ClusterRdd(val rdd: RDD[Cluster]) {
  rdd.name = "ClusterRdd"

  def cache() = rdd.cache()

  def toTextFile(uri: URI) = {
    val lines = rdd.map(c => c.toString)
    lines.saveAsTextFile(uri.toString)
  }

  def toObjectFile(uri: URI) = {
    rdd.saveAsObjectFile(uri.toString)
  }

  def calcAverageInformation(alpha: Double, minIdentity: Double) = {
    val info = calcInformation(alpha, minIdentity)
    val sum = info.reduce(_+_)
    val count = rdd.count()
    sum/count
  }

  def calcInformation(alpha: Double, minIdentity: Double) = {
    rdd.map{cluster =>
      information(cluster.members.map(_.identity), minIdentity, alpha)
    }
  }
}

object ClusterRdd {

  type CenterName = String
  type MemberName = String

  def groupEntries(entries: RDD[(Long, Member)]): ClusterRdd = {
    val grouped = entries.groupByKey()
    val clusters = grouped.map{case (centerId, iterable) =>
      val (centerIt, membersIt) = iterable.partition(_.id.id == centerId)
      val members = membersIt.toVector

      if (centerIt.size != 1) {
        throw new IllegalStateException("Zero or several centers for id %d . Center: %s Members: %s".format(centerId, centerIt.mkString(", "), members.mkString(", ")))
      }
      val center = centerIt.head.id
      Cluster(center, members)
    }
    new ClusterRdd(clusters)
  }

  def fromClusterGraph(graph: ClusterGraph): ClusterRdd= {
    val entries = graph.vertices.map{case (id,(name,assignment)) =>
      assignment match {
        case Center => (id, Member(GeneId(id, name), 1.0))
        case Assigned(identity, centerId) => (centerId, Member (GeneId(id, name), identity))
        case _ => throw new IllegalArgumentException("Graph contains unassigned nodes: " + (id,(name,assignment)))
      }
    }
    groupEntries(entries)
  }

  private def readUclustOutput(sc : SparkContext, file: URI) : RDD[(MemberName, CenterName)] = {
    val lines = sc.textFile(file.toString, SimpleSpark.defaultPartitions(sc)).map(new UClustLine(_))
    val uclustPairs = lines.flatMap {line =>
      line.tpe match {
        case "S" => Some((line.query, line.query)) //Seed
        case "H" => Some((line.query, line.target)) //Hit
        case "C" => None
        case "L" | "D" | "N" => throw new IllegalArgumentException ("Uclust data contains library data: " + line.line)
        case "R" => throw new IllegalArgumentException ("Uclust data contains rejects: " + line.line)
        case _ => throw new IllegalArgumentException ("Uclust data contains an unknown line type: " + line.line)
      }
    }
    uclustPairs
  }

  private def readCdhitOutput(sc : SparkContext, file: URI) : RDD[(MemberName, CenterName)] = {
    val clusters = SimpleSpark.delimitedFile(sc, file, ">Cluster ").filter(_._2.length > 0)
    clusters.flatMap {element =>
      val lines = element._2.split("[\\r\\n]+").toVector
      // first line is cluster number
      val content = lines.tail
      if (content.size > 0) {
        // extract ids
        val r = "nt, >?(.+)\\.\\.\\. .*".r
        val ids = content.map{s =>
          try {
            r.findFirstMatchIn(s).get.group(1)
          } catch {
            case e : Exception => throw new RuntimeException("Could not extract ID from: " + s, e)
          }
        }
        val center = ids.head
        val members = ids.tail

        // create pairs
        val pairs = members.map((_, center)) :+ ((center, center))
        pairs
      } else {
        Vector()
      }
    }
  }

  def groupNamePairs(uclustPairs: RDD[(MemberName, CenterName)], dna: RDD[(MemberName,DnaSeq)]) : RDD[(CenterName, Iterable[DnaSeq])] = {
    uclustPairs.leftOuterJoin(dna).map{case (memberName, (centerName, dnaOpt)) =>
      if (dnaOpt.isEmpty) {
        throw new IllegalArgumentException("Cannot find name from uclust in fasta file: " + memberName)
      }
      (centerName, dnaOpt.get)
    }.groupByKey()
  }

  def calcIdentity(grouped: RDD[(CenterName, Iterable[DnaSeq])], parameters:AlignmentParameters) = {
    val clusters = grouped.map{case (centerName, iterable) =>
      val (centerIt, membersIt) = iterable.partition(_.name == centerName)
      val membersVector = membersIt.toVector

      if (centerIt.size != 1) {
        throw new IllegalStateException("Zero or several centers for name %s : %s".format(centerName, centerIt.mkString(", ")))
      }
      val centerDna = centerIt.head

      val members = membersVector.map{dna =>
        Member(GeneId(dna.id, dna.name), JalignerUtils.calcIdentity(dna, centerDna, parameters))
      }
      val center = GeneId(centerDna.id, centerName)

      Cluster(center, members)
    }
    new ClusterRdd(clusters)
  }

  def fromUclustOutput(sc : SparkContext, uclustData: URI, genes : RDD[DnaSeq], parameters:AlignmentParameters): ClusterRdd= {
    val uclustPairs = readUclustOutput(sc, uclustData)
    val dna = genes.map(dna => (dna.name,dna))
    val grouped = groupNamePairs(uclustPairs, dna)
    calcIdentity(grouped, parameters)
  }

  def fromCdhitOutput(sc : SparkContext, cdhitData: URI, genes : RDD[DnaSeq], parameters:AlignmentParameters): ClusterRdd= {
    val cdhitPairs = readCdhitOutput(sc, cdhitData)
    val dna = genes.map(dna => (dna.name,dna))
    val grouped = groupNamePairs(cdhitPairs, dna)
    calcIdentity(grouped, parameters)
  }

  def fromObjectFile(sc : SparkContext, file: File) = {
    sc.objectFile(file.getAbsolutePath)
  }
}

case class GeneId(id:Long, name:String)
case class Member(id : GeneId, identity : Double)
case class Cluster(center : GeneId, members: Vector[Member])
