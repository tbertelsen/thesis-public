package apps

import java.io.File
import java.net.URI
import java.text.DecimalFormat

import apps.GeneClusteringArgs.{Benchmark, Cluster}
import apps.SparkClustering.{ClusterGraph, InputGraph, PartiallyAssignedGraph}
import apps.Timer.time
import com.sun.org.apache.xpath.internal.operations.Bool
import org.apache.spark.SparkContext
import org.apache.spark.graphx.{TripletFields, Graph}
import org.apache.spark.network.sasl.SparkSaslClient
import org.rogach.scallop.{Subcommand, ScallopConf}
import sequences.jaligner.{JalignerUtils, AlignmentParameters}
import sequences.{ClusterRdd, DnaSeq, KmerProfile, DNA}
import utils.IO.{safeFileName, fileNameFromURI, fileOrURI, serialize}
import scala.language.postfixOps
import org.rogach.scallop._

/**
 * Created by tobber on 15/3/15.
 */
class GeneClusteringArgs(stringArgs: Seq[String])  extends ScallopConf(stringArgs) {
  printedName = "GeneClustering"
  val cluster = new GeneClusteringArgs.Cluster
  val benchmark = new GeneClusteringArgs.Benchmark
  ConfHelp.defaultTweak(this)
}

object GeneClusteringArgs {

  val defaultAlignmentParameters = AlignmentParameters.usearch
  val customAlignmentParametersName = "custom"

  val noCheckFileConverter = singleArgConverter[File](s => new File(s))
  // Should be newline, but scallop does not support it
  val NL = " "

  @SerialVersionUID(362388923467L)
  class Benchmark extends Cluster("benchmark") {
    descr("Bechmark gene clustering")

    val repetitions = opt[Int](default = Some(3),
      descr = "The number of repetitions to do")

    val warmUp = opt[Int](default = Some(0),
      descr = "The number of warm up rounds to do")

    val saveClusters = toggle(default = Some(false), noshort = true,
      descrYes = "Save the cluster assignments to files.",
      descrNo = "[Default] Do not save the cluster assignments. Only save benchmark stats.")

    val clusterTextUri = opt[URI](required = false, noshort = true,
      descr = "The URI to save cluster assignments as text.")(singleArgConverter(fileOrURI(_)))
    val clusterObjectsUri = opt[URI](required = false, noshort = true,
      descr = "The URI to save cluster assignments as serialized objects.")(singleArgConverter(fileOrURI(_)))

  }

  @SerialVersionUID(2742863728L)
  class Cluster(name : String = "cluster") extends Subcommand(name) with Serializable {
    descr("Cluster Genes")

    mainOptions = Seq(inputFile, outputDir, master, lambda, alpha)

    val inputFile = opt[URI](required = true, short = 'f',
    descr = "The fasta file containing the genes to cluster")(singleArgConverter(fileOrURI(_)))
    val outputDir = opt[File](required = true, short = 'o', validate = (dir) => dir.isDirectory || dir.mkdirs(),
    descr = "The directory to output results to")(noCheckFileConverter)
    val master = opt[String](required = true, short = 'm',
    descr = "The URL to the spark master.")
    val k = opt[Int](default = Some(15), validate = k => 0 < k && k <= KmerProfile.MAX_K,
    descr = "The length of the kmers used for the proxy")
    val expandRounds = opt[Int](default = Some(0), validate = 0 <=, noshort = true,
    descr = "Number of \"expand rounds\" in which to search for missed neighbors. " + NL + "Warning: Seriously harms performance")
    val findProxyCenters = toggle(default = Some(false), noshort = true, name = "proxy-centers", prefix = "no-",
      descrYes = "Find centers based on the proxy. Will likely do fewer alignments, and might be faster. It is recommended, to set 'exact-corr' when using this option",
      descrNo = "[Default] Find centers based on actual alignment. Gives better results, but might be slower in densely connected data sets.")

    val lambda = opt[Double](default = Some(60.0), validate = 1.0 <,
    descr = "Tuning parameter for proxy calculation. The higher the value the higher the accuracy and precision, but it also slightly harms performance.")
    val alpha = opt[Double](default = Some(1.0), validate = a => a > 0.01 && 1 < 100,
    descr = "The exponent of the information function.")

    val minIdentity = opt[Double](default = Some(0.95), short = 'i', name = "minID",
    descr = "The minimal fractional identity between a cluster's center and its members")

    val expectedMinCorrelation = opt[Double](default = Some(0.45), name = "exp-corr",
      validate = c => 0.0 < c && c <= 1.0,
    descr = "The expected minimal correlation for (almost) every pair with an identity above 'minIdentity'. " +
            "This is used to calculate information inspired quality (IIQ) based on proxies.")

    val calcExactCorr = toggle(default = Some(false), noshort = true, name = "exact-corr", prefix = "no-",
      descrYes = "Calculate exact correlations for all pairs with an approximate correlation above 'approx-corr-cutoff'.",
      descrNo = "[Default] Only filter pairs based on the approximate correlation cutoff.")

    val exactCorrCutoff  = opt[Double](default = Some(math.max(0.1, 0.10)), short = 'e',
      validate = c => 0.0 < c && c < expectedMinCorrelation (),
      descr = "Pairs with an exact correlation below this will never be tested for identity."
    )

    val approxCorrCutoff = opt[Double](default = Some(math.max(0.1, 0.05)), short = 'c',
      validate = c => 0.0 <= c && c <= exactCorrCutoff (),
      descr = "Pairs with an approximate correlation below this will never be tested for identity. "
    )

    val matchScore = opt[Double](default = Some(defaultAlignmentParameters.matchScore), name = "match",
    descr = "The match score to use when calculating identities" + NL +
      "This is only used when 'alignmentParameters = " + customAlignmentParametersName)
    val mismatchScore = opt[Double](default = Some(defaultAlignmentParameters.mismatchScore), name = "mismatch",
    descr = "The mismatch core to use when calculating identities" + NL +
      "This is only used when 'alignmentParameters = " + customAlignmentParametersName)
    val gapOpen = opt[Double](default = Some(defaultAlignmentParameters.gapOpen),
    descr = "The gap open penalty to use when calculating identities" + NL +
      "This is only used when 'alignmentParameters = " + customAlignmentParametersName)
    val gapExtend = opt[Double](default = Some(defaultAlignmentParameters.gapExtend),
    descr = "The gap extend penalty to use when calculating identities" + NL +
      "This is only used when 'alignmentParameters = " + customAlignmentParametersName)
    val minCover = opt[Double](default = Some(defaultAlignmentParameters.minCoverFraction),
      descr = "The fraction of the shortest gene in an alignment, that must be covered by the alignment" + NL +
              "This is only used when 'alignmentParameters = " + customAlignmentParametersName)

    val alignmentParameters = opt[String](default = Some(defaultAlignmentParameters.name), name = "alignParams",
      validate = (name => AlignmentParameters.predefined.contains(name.toLowerCase) || name == customAlignmentParametersName),
    descr = "The type of alignment parameters to use." + NL +
      "Must be one of %s, or %s".format(AlignmentParameters.predefined.keys.mkString(", "), customAlignmentParametersName))

    ConfHelp.defaultTweak(this)

    def uniqueName() = {
      // Number format:
      val df = new DecimalFormat("#.##");
      def fd(d:Double) = df.format(d)
      def fi(i:Int) = df.format(i)
      // Content
      val file = fileNameFromURI(inputFile())
      val proxyC = if (findProxyCenters()) "_proxyC" else ""
      val exact = if (calcExactCorr()) "_exactCorr" else ""
      val expand = if (expandRounds() > 0) "_expand=" + fi(expandRounds()) else ""
      "%s_l=%s_a=%s".format(file, fd(lambda()), fd(alpha())) + proxyC + exact + expand
    }

    def getAlignmentParameters() = {
      if (alignmentParameters() == customAlignmentParametersName) {
        AlignmentParameters(matchScore(), mismatchScore(), gapOpen(), gapExtend(), minCover(), customAlignmentParametersName)
      } else {
        AlignmentParameters.fromString(alignmentParameters()).get
      }
    }

    def tuning() = {
      new Tuning (
        k = k (),
        lambda = lambda (),
        calcExact = calcExactCorr (),
        exactCutoff = exactCorrCutoff (),
        approxCutoff = approxCorrCutoff (),
        expandedSearchRounds = expandRounds ())
    }
  }
}

object GeneClustering {

  def main(argStrings: Array[String]) {
    val args = new GeneClusteringArgs(argStrings)
    args.subcommand match {
      case Some(args.cluster) => runCluster(args.cluster)
      case Some(args.benchmark) => runBenchmark(args.benchmark)
      case Some(subcommand) => throw new IllegalArgumentException("Unknown command: " + subcommand)
      case None => throw new IllegalArgumentException("Please specify a command")
    }
  }

  def runCluster(args: Cluster): Unit = {
    val sc = SimpleSpark.startSpark(args.master(), "Gene Clustering on " + fileNameFromURI(args.inputFile()))
    doCluster(args, sc)
    sc.stop()
  }

  def runBenchmark(args: Benchmark) = {
    println(args.summary)
    val sc = SimpleSpark.startSpark(args.master(), "Gene Clustering on " + fileNameFromURI(args.inputFile()))
    val meta = Meta(args, sc)
    println(meta)

    time("Warm up"){() =>
      for (i <- 1 to args.warmUp()) {
        doCluster(args, sc)
      }
    }

    val timings = (for (i <- 1 to args.repetitions()) yield {
      println("Round %d of %d.".format(i, args.repetitions()))
      Timer.getSeconds{() =>
        val clusters = doCluster(args, sc)
        clusters.cache()
        clusters.rdd.count()
        clusters
      }
    }).toVector
    val files = Files(args, meta)
    // Save metadata
//    serialize(files.meta, Meta(files, args))
    // Save cluster
    if (args.saveClusters()) {
      val clusters = timings.head._1
      clusters.toObjectFile(args.clusterObjectsUri())
      clusters.toTextFile(args.clusterTextUri())
    }
    // Save benchmark
    // FIXME: Save results as binary data
    // FIXME: Manually extract important arguments and save them

    val avgInfo = timings.map{_._1.calcAverageInformation(args.alpha(), args.minIdentity() )}
    val seconds = timings.map(_._2)
    val results = new BenchmarkResults(meta, seconds, avgInfo)
//    println("Seconds = " + seconds)
//    println("Saving to " + files.bench)
    serialize(files.bench, results)
    utils.IO.printToFile(files.benchText){out =>
      out.println(seconds)
      out.println(avgInfo)
      out.println(results.meta)
    }
    utils.IO.printToFile(files.statsText){out =>
      out.println(seconds)
      out.println(avgInfo)
    }
    readLine("Press enter to exit"); sc.stop()
  }

  def doCluster(args: Cluster, sc: SparkContext) : ClusterRdd = {
    val sparkCorrelation = new SparkCorrelation(args.tuning())
    val correlationGraph = sparkCorrelation.createCorrelationGraph(sc, args.inputFile())
    val clusterGraph : ClusterGraph = if (args.findProxyCenters()) {
      val engine = new SparkProxyClustering(args.alpha(), args.minIdentity(), args.expectedMinCorrelation(), args.getAlignmentParameters())
      engine.cluster(correlationGraph)
    } else {
      val identityGraph = GeneClustering.toIdentityGraph(correlationGraph, args)
      val engine = new SparkIdentityClustering(args.alpha(), args.minIdentity())
      engine.cluster(identityGraph)
    }
    ClusterRdd.fromClusterGraph(clusterGraph)
  }

  def toIdentityGraph(correlationGraph : Graph[DnaSeq, Double], args: Cluster) : InputGraph[String]  = {
    val minId = args.minIdentity()
    val alignParams = args.getAlignmentParameters()
    toIdentityGraph(correlationGraph, minId, alignParams)
  }

  def toIdentityGraph(correlationGraph : Graph[DnaSeq, Double], minId: Double, alignParams : AlignmentParameters) : InputGraph[String] = {
    val identityEdges = correlationGraph.mapTriplets({t =>
      JalignerUtils.calcIdentity(t.srcAttr, t.dstAttr, alignParams)
    }, TripletFields.All).edges.cache()
    val preCount = identityEdges.count()
    val newEdges = identityEdges.filter(_.attr >= minId).cache()
    val postCount = newEdges.count()
    println("%d / %d = %d %% gene pairs analyzed".format(preCount, postCount, (preCount*100/postCount)))
    val newVertices = correlationGraph.vertices.mapValues(_.name)
    Graph(newVertices, newEdges)
  }
}

@SerialVersionUID(4578873L)
case class BenchmarkResults(meta: Meta, seconds: Vector[Double], avgInformation : Vector[Double]) extends Serializable

@SerialVersionUID(765357374419338428L)
case class Meta(filename: String,
      cores: Int,
      proxyCenters: Boolean,
      calcExactCorr: Boolean,
      expandRounds: Int,
      lamda: Double,
      alpha: Double
      ) extends Serializable

object Meta {
  def apply (args: Benchmark, sc: SparkContext) = {
    val file = fileNameFromURI(args.inputFile())
    val cores = sys.env.getOrElse("dk_tbertelsen_cores", sc.defaultParallelism.toString).toInt
    println("cores = " + cores)
    new Meta(file, cores, args.findProxyCenters(), args.calcExactCorr(), args.expandRounds(), args.lambda(), args.alpha())
  }
}

@SerialVersionUID(401983824354446320L)
class Files(dir:File, name:String) extends Serializable {
  def file(extension: String) = {
    new File(dir, name + extension).getAbsoluteFile
  }

  val meta = file(".meta")
  val clusterRdd = file(".clusterRdd")
  val clusterRddText = file(".clusterRdd.txt")
//  val clusterRddStats = file(".clusterRddStats")
  val bench = file(".bench")
  val benchText = file(".bench.txt")
  val statsText = file(".stats.txt")
  val stats = file(".stats")
}

object Files {
  def apply(args:Cluster) = new Files(args.outputDir(), args.uniqueName())

  def apply(args:Cluster, meta: Meta) = {
    val metaString = meta.toString
    val paramString = metaString.substring(metaString.indexOf('(') + 1, metaString.lastIndexOf(')'))
    new Files(args.outputDir(), safeFileName(paramString))
  }
}
