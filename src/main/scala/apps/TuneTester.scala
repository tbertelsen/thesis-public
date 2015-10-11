package apps

import java.io._
import java.net.URI
import java.util.Scanner

import apps.Timer._
import geneio.{FastaUtils, FastaSpark}
import org.apache.spark.SparkContext
import org.rogach.scallop._
import sequences.jaligner.{AlignmentParameters, JalignerUtils}
import sequences.{DnaSeq, KmerProfile}
import splot._
import utils.IO._
import utils.{TupleRef, Greek}

import scala.io.Source

/**
 * Created by tobber on 12/3/15.
 */

@SerialVersionUID(1161875881837644095L)
class TuneTesterArgs(stringArgs: Seq[String]) extends ScallopConf(stringArgs) with Serializable {
  printedName = "thesis"

  val run = new TuneTesterArgs.Run()
  val plot = new TuneTesterArgs.Plot()
  val count = new TuneTesterArgs.Count()

  ConfHelp.defaultTweak(this)
}

object TuneTesterArgs {

  @SerialVersionUID(2096228807622544782L)
  class Run extends TuneTesterCommand("run") {
    descr("Calculate tuning test results")
    val input = opt[String](required = true)
    val master = opt[String](required = true)

    def inputFile() = fileOrURI(input())
  }

  @SerialVersionUID(88525796345422026L)
  class Count extends TuneTesterCommand("count") {
    descr("Count actual positives")
    val input = opt[String](required = true)
    def inputFile() = new File(input())
    validate(proxy){if (_) Left("Proxy count not implemented") else Right(Unit)}
  }

  @SerialVersionUID(702078999598829000L)
  class Plot extends TuneTesterCommand("plot") {
    val plotOmission = opt[Boolean](default = Some(false), short = 'o')

    descr("Plot tuning test results")
  }

  abstract class TuneTesterCommand(name: String) extends Subcommand(name) with Serializable {
    val workdir = opt[String](required = true)

    val proxy = opt[Boolean](default = Some(false))

    def workdirFile() = {
      val f = new File(workdir()).getAbsoluteFile
      f.mkdirs()
      f.mkdir()
      f
    }

    def resultFile() = new File(workdirFile(), "tuneTestResults%s.serial".format(idOrProxy()))

    def countFile() = new File(workdirFile(), "count%s.txt".format(idOrProxy()))

    def idOrProxy() = {
      if (proxy()) "Proxy" else "Id"
    }

    ConfHelp.defaultTweak(this)
  }
}


object TuneTester {

  val k = 15
  val minId = 0.95
  var alignParams: AlignmentParameters = AlignmentParameters.usearch

  def runCount(args: TuneTesterArgs.Count): Unit = {
    if (args.proxy()) {
      // When implementing remember to enable argument
      ???
    } else {
      val count = bruteForceIdentityLocal(args.inputFile())
      println("%d positives found".format(count))
      utils.IO.printToFile(args.countFile()) { p => p.println(count)}
    }
  }

  def readCount(args: TuneTesterArgs.TuneTesterCommand): Int = {
    val countString = Source.fromFile(args.countFile()).getLines().next()
    countString.toInt
  }

  def main(argStrings: Array[String]) {
    val args = new TuneTesterArgs(argStrings)
    args.subcommand match {
      case Some(args.count) => runCount(args.count)
      case Some(args.run) => runTests(args.run)
      case Some(args.plot) => plotTest(args.plot)
      case Some(x) => args.errorMessageHandler("Unknown command: " + x)
      case None => args.errorMessageHandler("Please specify a command.")
    }
  }


  def runTests(args: TuneTesterArgs.Run): Unit = {
    val inputFile = args.inputFile()
    val resultFile = args.resultFile()
    val master = args.master()
    val calcId = !args.proxy()
    val exacts = List(args.proxy())

    val lambdas = (10 to 80 by 10).toList
    val cutoffs = List(0.01, 0.05, 0.10, 0.15, 0.20) //(0.01 to 0.1 by 0.01) ++ (0.1 to exactCutoff by 0.05)
    val expandRounds = List(0,1)
    val testRounds = 3

    // Ensure nice plots
    if (cutoffs.length * expandRounds.length % 5 != 0) {
      throw new RuntimeException("Please use 5, 10, or 15 series")
    }

    val sc = SimpleSpark.startSpark(master, "Test Tuning")

    println("sc.defaultParallelism = " + sc.defaultParallelism)

    // Exercise spark and file system. Otherwise fist run will be slow:
    for (i <- 1 to 2) {
      println("Warm up %d / 2".format(i))
      val tuning = new Tuning(k = k, exactCutoff = 0.1, calcExact = true, lambda = lambdas.last, approxCutoff = cutoffs.last, expandedSearchRounds = expandRounds.last)
      val engine = new SparkCorrelation(tuning)
      val proxyGraph = engine.createCorrelationGraph(sc, inputFile)
      val idGraph = GeneClustering.toIdentityGraph(proxyGraph, minId, alignParams)
      idGraph.edges.count()
    }

    val caseCount = lambdas.length * cutoffs.length * expandRounds.length
    var caseI = 0
    val testResults = (for (lambda <- lambdas; cutoff <- cutoffs; expandRound <- expandRounds; exact <- exacts) yield {
      caseI = caseI + 1
      val tuning = new Tuning(k = k, exactCutoff = cutoff, calcExact = exact, lambda = lambda, approxCutoff = cutoff, expandedSearchRounds = expandRound)
      val engine = new SparkCorrelation(tuning)

      Timer.log("Run %3d/%d : %s".format(caseI, caseCount, tuning.toString()))

      val testRoundResults = (for (t <- 1 to testRounds) yield {
        Timer.getSeconds[Long] { () =>
          val proxyGraph = engine.createCorrelationGraph(sc, inputFile)
          if (calcId) {
            val idGraph = GeneClustering.toIdentityGraph(proxyGraph, minId, alignParams)
            idGraph.edges.count()
          } else {
            proxyGraph.edges.count()
          }
        }
      }).toList
      sc.getPersistentRDDs.values.foreach(_.unpersist(true))
      (new TuneTestCase(lambda, cutoff, expandRound, exact), new TuneTestCaseResults(testRoundResults))
    }).toList

    val tuneTestRun = new TuneTestRunResults(testResults, lambdas, cutoffs, cutoffs, expandRounds, exacts, testRounds, inputFile, args.proxy(), minId, alignParams, k)
    serializeTestRun(tuneTestRun, resultFile)
  }

  def plotTest(args: TuneTesterArgs.Plot) = {
    val resultFile = args.resultFile()
    val testRun = deserializeTestRun(resultFile)
    val testResults = testRun.testResults.toMap

    val figDir = new File(args.workdirFile().getAbsolutePath + "/fig-tuning-test/" + resultFile.getName + "/")
    figDir.mkdirs()
    utils.IO.printToFile(new File(figDir, "data.txt")){p =>
      for (res <- testRun.testResults) {
        p.println(res._1 + " => \t" + res._2.positives + "\t" + res._2.seconds)
      }
    }

    println("Extracting Series")
    val plotSeries = (for (expandRound <- testRun.expandRounds; cutoff <- testRun.approxCutoffs; exact <- testRun.exacts) yield {
      val zipped = for (lambda <- testRun.lambdas) yield {
        // List of pairs to plot
        // (lambda, positive), (lambda, avgTime), (Lambda, minTime))
        val data = testResults.get(new TuneTestCase(lambda, cutoff, expandRound, exact)).get
        ((lambda.toDouble, data.avgPos), (lambda.toDouble, data.avgSec), (lambda.toDouble, data.seconds.min))
      }
      ("%d @ %d%%".format(expandRound, (cutoff * 100).toInt), zipped.unzip3)
    }).toList

    val positiveSeries = plotSeries map { case (name, data) => (name, data._1)}
    val secSeries = plotSeries map { case (name, data) => (name, data._2)}
    val minSecSeries = plotSeries map { case (name, data) => (name, data._3)}

    println("Plotting")
    createPlots(positiveSeries, "True Positives", figDir, "positives")
    createPlots(secSeries, "Runtime (sec)", figDir, "runtime")
    createPlots(minSecSeries, "Runtime (sec)", figDir, "runtimeMin")
    if (args.plotOmission()) {
      val actualPositive = testResults.values.map(_.positives.max).max
      val omissionSeries = positiveSeries map {case (name, list) =>
        (name, list map {case (lambda, pos) => (lambda, 1 - pos / actualPositive)})
      }
      val falseNegativeSeries = positiveSeries map {case (name, list) =>
        (name, list map {case (lambda, pos) => (lambda, actualPositive - pos)})
      }
      createPlots(omissionSeries, "False Omission Rate", figDir, "omission")
      createPlots(falseNegativeSeries, "False Negatives", figDir, "falsenegatives")
    }
  }

  def createPlots(series: List[(String, List[(Double, Double)])], yLabel: String, outDir: File, filename: String) = {
    val plot = linePlot(Greek.lambda, yLabel)
    series.foreach { tup => plot += points(tup._2, tup._1)}
    plot.save(new File(outDir, filename + ".png"))
    plot.log().save(new File(outDir, filename + "Log.png"))
  }

  def bruteForceIdentityLocal(file: File) : Long = {
    if (!file.getName.endsWith(".align")) {
      throw new IOException("Illegal file type. Must be '.align'")
    }
    var count = 0
    val sc = new Scanner(file)
    try {
      while (sc.hasNextLine) {
        val line = sc.nextLine().split("\t")
        val align = line(2).toDouble
        if (align > minId * 100) {
          count = count + 1
        }
      }
    } finally {
      sc.close()
    }
    count
  }

//  def bruteForceCorrelationLocal(file: File) : Long = {
//    val toKmer = KmerProfile(k) _
//    val dna = time("Reading " + file.getAbsolutePath)(() =>
//      FastaUtils.readFastaFile(file)
//    )
//    val kmers = time("Parsing kmers ") { () =>
//      dna.par.map(d => (d.id, toKmer(d.content))).toList
//    }
//    val countSum = time("Comparing kmers") { () =>
//      bruteForceCountPairsLocal(kmers){case ((id1,kmer1),(id2,kmer2)) =>
//        id1 < id2 && kmer1.cosineSimilarity(kmer2) > exactCutoff
//      }
//    }
//    countSum
//  }
//
//  def bruteForceCountPairsLocal[T](list : List[T])(pred : (T,T) => Boolean) = {
//    val n = list.length.toLong
//    val steps = 1000L
//    val stepsSize = math.max(2,n / steps)
//    val progress = new SyncProgress(steps)
//    progress.start()
//    val countSum = list.par.zipWithIndex.map { case (elem1, index) =>
//      if (index % stepsSize == 0L) {
//        progress.tick()
//      }
//      list.count { elem2 => pred(elem1, elem2)}
//    }.reduce(_ + _)
//    progress.end()
//    countSum
//
//  }

    //
  //  def bruteForceCorrelationOnSpark(sc: SparkContext, file: File) = {
  //    val k_ = k
  //    val exactCutoff_ = exactCutoff
  //    val toKmer = KmerProfile(k_) _
  //    val dna = FastaSpark.readFastaFile(sc, file.getAbsolutePath)
  //    // Use a small repartition, since it will be squared by the cartesian operation
  //    val kmers = dna.map(d => (d.id, toKmer(d.content))).repartition(20).cache()
  //    val pairs = kmers.cartesian(kmers).mapPartitions { seq =>
  //      val c = seq.count{case ((id1, kmer1), (id2, kmer2))  =>
  //        id1 < id2 && kmer1.cosineSimilarity(kmer2) > exactCutoff_
  //      }
  //      Iterator(c)
  //    }
  //    pairs.reduce(_ + _)
  //  }

  def serializeTestRun(results: TuneTestRunResults, file: File) = {
    println(file)
    time("Serializing TestRun.") { () =>
      val oos = new ObjectOutputStream(new FileOutputStream(file))
      oos.writeObject(results)
      oos.close
    }
  }

  def deserializeTestRun(file: File) = {
    println(file)
    time("Deserializing TestRun.") { () =>
      val ois = new ObjectInputStream(new FileInputStream(file))
      val results = ois.readObject.asInstanceOf[TuneTestRunResults]
      ois.close
      results
    }
  }

}

@SerialVersionUID (311885327382637795L)
class TuneTestCase(
      val lambda: Double,
      val sampleCutoff: Double,
      val expandedSearchRounds: Int,
      val calcExact: Boolean) extends TupleRef with Serializable {
  override protected def asTuple() = (lambda, sampleCutoff, expandedSearchRounds, calcExact)
}

@SerialVersionUID(648553101369927014L)
class TuneTestCaseResults(val results: List[(Long, Double)]) extends Serializable {
  val (positives, seconds) = results.map(tup => (tup._1.toDouble, tup._2)).unzip
  val (avgPos) = positives.sum / positives.length
  val (avgSec) = seconds.sum / seconds.length
}


@SerialVersionUID(208410353426912587L)
class TuneTestRunResults(
      val testResults: List[(TuneTestCase, TuneTestCaseResults)],
      val lambdas: List[Int],
      val approxCutoffs: List[Double],
      val exactCutoffs: List[Double],
      val expandRounds: List[Int],
      val exacts: List[Boolean],
      val testRounds: Int,
      val inputFile: URI,
      val proxy: Boolean,
      val minId: Double,
      val alignParams: AlignmentParameters,
      val k: Int) extends Serializable {
}
