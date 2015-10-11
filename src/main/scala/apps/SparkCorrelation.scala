package apps


import java.io._
import java.net.URI

import apps.SimpleSpark.{mediumCache, defaultPartitions}
import geneio.FastaSpark
import org.apache.log4j.Level

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.graphx.EdgeDirection.Either
import org.apache.spark.graphx.{VertexId, Edge, Graph}
import org.apache.spark.rdd.RDD
import sequences.{DnaSeq, KmerProfile, KmerProfileNormalized}

import scala.language.postfixOps


object SparkCorrelationTester {
//
//  def printAllPosNeg(sc : SparkContext, file : String = SimpleSpark.fa1495, require : Double = 0.9, rounds : Seq[Int] = 1 to 10): Unit = {
//    val kmers = loadKmers(sc, file)
//    val complete = completeCorrelationKmer(kmers).cache()
//    def innerAllPosNeg(lambda : Double = 5.0) = {
//      println()
//      printf("Factor = %.1f%n", lambda)
//      printf("factor \tcutoff \trequire \tfalsePositive \ttruePositive \tfalseNegative \ttrueNegative")
//      val sample = sampleCorrelationKmer(kmers, lambda)
//      val points = joinCorrPoints(complete, sample)
//      val pointsList = points.collect().toList.par
//      println("Got list")
//      def countPosNeg(cutoff: Double = 0.6) = {
//        val falsePositive = pointsList.count { case (act, aprx) => act < require && aprx > cutoff}
//        val truePositive = pointsList.count { case (act, aprx) => act >= require && aprx > cutoff}
//        val trueNegative = pointsList.count { case (act, aprx) => act < require && aprx <= cutoff}
//        val falseNegative = pointsList.count { case (act, aprx) => act >= require && aprx <= cutoff}
//        printf("%.1f\t%f\t%f\t%d\t%d\t%d\t%d%n", lambda, cutoff, require, falsePositive, truePositive, falseNegative, trueNegative)
//      }
//      println();
//      for (i <- 0.0 to 0.9 by 0.1) {
//        countPosNeg(i)
//      }
//    }
//    for (i <- rounds) {innerAllPosNeg(5.0 * math.pow(2, i))}
//
//  }
//
//  def time210(sc : SparkContext, lambda : Double) = {
//    time(sc, lambda, SimpleSpark.fa210)
//  }
//
//  def time1495(sc : SparkContext, lambda : Double) = {
//    time(sc, lambda, SimpleSpark.fa1495)
//  }
//
//  def timeComplete210(sc : SparkContext) = {
//    timeComplete(sc, SimpleSpark.fa210)
//  }
//
//  def time(sc : SparkContext, lambda : Double, file: String) = {
//    val startTime = System.currentTimeMillis();
//    val kmers = loadKmers(sc, file)
//    val sample = sampleCorrelationKmer(kmers, lambda)
//    val count = sample count()
//    val duration = (System.currentTimeMillis() - startTime) / 1000.0
//    printf("Sampling for %.1f in %.2f s%n", lambda, duration)
//  }
//
//  def timeComplete(sc : SparkContext, file : String) = {
//    val startTime = System.currentTimeMillis();
//    val kmers = loadKmers(sc, file)
//    val complete = completeCorrelationKmer(kmers)
//    val count = complete count()
//    val duration = (System.currentTimeMillis() - startTime) / 1000.0
//    printf("Complete in %.2f s%n", duration)
//  }
//
//  def plot210(sc : SparkContext, lambda : Double, filt : ((Double,Double)) => Boolean = x => true) = {
//    plotPoints(sc, SimpleSpark.fa210, lambda, filt)
//  }
//
//  def timeComplete1495(sc : SparkContext) = {
//    timeComplete(sc, SimpleSpark.fa1495)
//  }
//
//  def plot1495(sc : SparkContext, lambda : Double, filt : ((Double,Double)) => Boolean = x => true) = {
//    plotPoints(sc, SimpleSpark.fa1495, lambda, filt)
//  }
//
//
//  def analyseKmerDistribution(sc: SparkContext, filePath : String, k : Int = 7, parts : Int = 16) = {
//    val genes = FastaSpark.readFastaFile(sc, filePath, parts)
//    val toKmer = KmerProfile(k)_
//    val doubles = genes flatMap {dna =>  toKmer(dna.content).normalize.activeIterator} map  (_._2)
//
//    println("Collecting data")
//    val data = doubles.filter(_ != 0.0).collect()
//    println("Starting plot")
//    val plot1 = histPlot("")
//    plot1 += hist(data)
//    plot1.save("histogram.png")
//
//    val plot2 = linePlot("", "Cumulative Frequency")
//    val lineData = data.sorted.zipWithIndex.map{t => (t._1, t._2.toDouble)}.toSeq
//    plot2 += points(lineData, "")
//    plot2.save("cumulative.png")
//
//    println("Counting zeroes")
//    val nAll = doubles.count()
//    val nNonZero = data.length
//    printf ("%d of %d = %.2f %% are non-zero%n", nNonZero, nAll, nNonZero * 100.0 / nAll)
//  }
//
//  def plotPoints(sc : SparkContext, file: String, lambda : Double, filt : ((Double,Double)) => Boolean) = {
//    val points = getCorrPoints(sc, file, lambda)
//    val plotPoints = points.filter(filt).collect()
//    easyPlot(plotPoints, "points.png")
//    points
//  }
//
//  def easyPlot(data : Seq[(Double, Double)], file : String) = {
//    val plot = scatterPlot("Actual Correlation", "Approximate Correlation")
//    plot += points(data, "")
//    plot.save(new File(file))
//  }
//
//  def getCorrPoints(sc : SparkContext, file: String, lambda : Double) = {
//    val kmers = loadKmers(sc, file)
//    val complete = completeCorrelationKmer(kmers)
//    val sample = sampleCorrelationKmer(kmers, lambda)
//    joinCorrPoints(complete, sample)
//  }
//
//  def joinCorrPoints(complete: RDD[((Long, Long), Double)], sample : RDD[((Long, Long), Double)]) = {
//    val points = complete.rightOuterJoin(sample).map{
//      case(k,(opt,v2)) => (opt.getOrElse(0.0),v2)
//    }
//    points
//  }
//
//
//  def loadKmers(sc: SparkContext, filePath : String, k : Int = 7) = {
//    val genes = FastaSpark.readFastaFile(sc, filePath)
//    val toKmer = KmerProfile(k)_
//    genes map {dna => (dna.id, toKmer(dna.content).normalize)}
//  }
//
}


/**
 * Created by tobber on 8/1/15.
 */
class SparkCorrelation(tuning : Tuning) {
  /**
   * Calculates approximate correlation
   * @param elements an RDD of (position, (vectorID, Value))
   * @return RDD of (IdPair,correlation)
   */
  def approxCorrelation(elements : RDD[(Int, (Long, Double))]) = {
    val lambda = tuning.lambda
    // Group values by position index.
    // Create a lot of small partitions, to spread out uneven load
    val crossColumns = elements.groupByKey(5 * SimpleSpark.defaultPartitions(elements.sparkContext))
    // "Emit" (flapmap) all addends
    val addends = crossColumns flatMap {case (pos, idValPairs) =>
      for ((id1,val1) <- idValPairs;
           (id2,val2) <- idValPairs;
           if id1 < id2;
           a = val1 * val2 * lambda;
           if a > math.random
      ) yield {
        ((id1, id2), math.max(1.0, a))
      }
    }
    // Sum all addends and calculate the correlation
    addends.reduceByKey{_ + _}.mapValues{_ / lambda}
  }

  def sampleCorrelationKmer(kmer: RDD[(Long, KmerProfileNormalized)]) = {
    approxCorrelation(explodeKmer(kmer))
  }

  def completeCorrelation(elements : RDD[(Int, (Long, Double))]) = {
    val crossColumns = elements.groupByKey()
    val contributions = crossColumns flatMap {case (pos, idValPairs) =>
      for (idVal1 <- idValPairs;
           idVal2 <- idValPairs;
           if idVal1._1 <= idVal2._1) yield {
        ((idVal1._1, idVal2._1), idVal1._2 * idVal2._2)
      }
    }
    contributions.reduceByKey{_ + _}
  }

  def explodeKmer(kmers: RDD[(Long, KmerProfileNormalized)]) = {
    kmers.flatMap { case (id, kmer) =>
      kmer.activeIterator.map {case (pos, value) => (pos, (id, value))}
    }
  }

  def completeCorrelationKmer(kmers: RDD[(Long, KmerProfileNormalized)]) = {
    completeCorrelation(explodeKmer(kmers))
  }

  def filterActualCorrelated(pairs: RDD[(Long, Long)], kmers: RDD[(Long, KmerProfileNormalized)]) = {
    val minCorr = tuning.exactCutoff
    val candidates = SimpleSpark.pairLookup(pairs, kmers)
    val actual = candidates.map{case (ids, (kmer1, kmer2)) =>
      val correlation = kmer1.cosineSimilarity(kmer2)
      (ids, correlation)
    }.filter{_._2 >= minCorr}
    actual
  }

  def createEdges(edgeData: RDD[((Long, Long), Double)]) = {
    edgeData filter {case ((id1, id2), _) => id1 != id2} map {case ((id1, id2), corr) => Edge(id1, id2, corr)}
  }

  def createCorrelationGraph(sc: SparkContext, uri : URI) : Graph[DnaSeq, Double] = {
    val simpleFileName = uri.toString.split('/').last
    val cutOff = tuning.approxCutoff;
    val genes = FastaSpark.readFastaFile(sc, uri).setName("Genes (%s)".format(simpleFileName)).persist(mediumCache)
    val k = tuning.k
    val toKmer = KmerProfile(k)_
    val kmers = genes map {dna => (dna.id, toKmer(dna.content).normalize)} setName("Kmers (%s)".format(simpleFileName)) persist(SimpleSpark.largeCache)
    val samplePairs = sampleCorrelationKmer(kmers).filter(_._2 >= cutOff)
    val edgeData = if (tuning.calcExact) {
      filterActualCorrelated(samplePairs.map(_._1), kmers)
    } else {
      samplePairs
    }
    val graph = if (tuning.newExpandAlgorithm) {
      secondExpandAlgorithm(sc, genes,  kmers, edgeData)
    } else {
      firstExpandAlgorithm(sc, genes, kmers, edgeData)
    }
    graph
  }

  def firstExpandAlgorithm(sc: SparkContext, genes: RDD[DnaSeq], kmers: RDD[(Long, KmerProfileNormalized)],edgeData: RDD[((Long, Long), Double)]) = {

    var allEdgeData = edgeData
    allEdgeData.setName("AllEdgeData").persist(mediumCache)
    val nodes = genes map {dna => (dna.id, dna)} setName "Nodes" persist(mediumCache)
    var graph = Graph(nodes, createEdges(edgeData))
    for (i <- 1 to tuning.expandedSearchRounds) {
      val allNeighborPairs = (neighbors: Array[VertexId]) => {
        for (a <- neighbors; b <- neighbors; if a < b) yield (a, b)
      }
      // TODO: Make more efficient. (e.g. do not test correlation of same edge twice)
      val newEdgesPairs = graph.collectNeighborIds(Either) flatMap{t => allNeighborPairs(t._2)} distinct()
      val newEdgesData = filterActualCorrelated(newEdgesPairs, kmers)

      allEdgeData = allEdgeData.fullOuterJoin(newEdgesData).map{case (ids,(opt1, opt2)) =>
        // Prioritize values in newEdgesData as they are always exact
        val corr = if (opt2.isDefined) opt2.get else opt1.get
        (ids, corr)
      }
      allEdgeData.setName("AllEdgeData" + i).persist(mediumCache)
      graph = Graph(nodes, createEdges(allEdgeData))
    }
    graph
  }

  /**
   * This is about 5 times as slow as the one above
   */
  def secondExpandAlgorithm(sc: SparkContext, genes: RDD[DnaSeq], kmers: RDD[(Long, KmerProfileNormalized)], edgeData: RDD[((Long, Long), Double)]) = {
    val minCorr = tuning.exactCutoff
    var edges = createEdges(edgeData)
    for (i <- 1 to tuning.expandedSearchRounds) {
      edges.persist(mediumCache)
      val kmerGraph = Graph(kmers, edges)
      val nodesStep1 = kmerGraph.collectNeighbors(Either)
      val graphStep1 = Graph(nodesStep1, edges)
      // TODO: Convert to aggregate messages:
      // TODO: Make the flatmap/unique/lessEq(Id)
      val nodesStep2 = graphStep1.collectNeighbors(Either)
      val newEdges = nodesStep2.filter(_._2.size > 0).flatMap{case (thisId, arrayOfArrays) =>
        // FlatMap to one-dimensional array:
        val all2StepNeighbors = arrayOfArrays.flatMap(array => array._2)
        // Keep only those with a lower ID (or the same)
        val candidates = all2StepNeighbors.filter{case (otherId, _) => otherId <= thisId}
        // Get unique
        val uniqueCandidatePlusThis = candidates.toMap
        val thisKmer = uniqueCandidatePlusThis.get(thisId).get
        val uniqueCandidate = uniqueCandidatePlusThis - thisId
        // Calculate correlation
        val matches = uniqueCandidate.map{case (otherId, otherKmer) =>
          Edge(otherId, thisId, thisKmer.cosineSimilarity(otherKmer))
        }
        // Keep only the best
        matches.filter(edge => edge.attr > minCorr)
      }
      val edgeUnion = edges union newEdges
      // Similar to edges.distinct() but ignore value
      edges = edgeUnion map (e => ((e.srcId, e.dstId),e)) reduceByKey((x,y) => x, defaultPartitions(sc)) map (_._2)
    }
    val nodes = genes map {dna => (dna.id, dna)}
    Graph(nodes, edges)
  }
}

@SerialVersionUID(347288164957518L)
case class Tuning(
       val k: Int = 14,
       val lambda : Double = 80,
       val calcExact: Boolean,
       val approxCutoff: Double = 0.20,
       val exactCutoff : Double = 0.35,
       val expandedSearchRounds : Int = 0,
       val newExpandAlgorithm : Boolean = false) extends Serializable {
  override def toString() = {
    "{k=%3d, exactCutoff = %.3f, approxCutoff = %.3f, lambda =%4.0f, calcExact=%5s expandSearchRounds =%2d}".format(
      k, exactCutoff, approxCutoff, lambda, calcExact.toString, expandedSearchRounds
    )
  }
}

//case class KmerElement(
//      val pos: Int,
//      val id: Long,
//      val value: Double
//      ) extends Product2 [Int,(Long,Double)] {
//  override def _1: Int = pos
//
//  override def _2: (Long, Double) = (id,value)
//}
