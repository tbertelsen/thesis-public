package apps

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import apps.ProxyArgs.kstats
import apps.Timer._
import sequences.jaligner.{AlignmentParameters, JalignerUtils}
import stats.ClassificationStats

import splot._
import geneio.{FastaUtils, AlignmentCache}
import sequences.{DnaSeq, DnaSeqBits, KmerProfile}
import utils.IO

import scala.collection.immutable.NumericRange
import scala.language.postfixOps

object ProxyTester {

  val geneBucketPath = "/eva/users/dkn957/data/"

  class DnaPack(val seq: DnaSeq, val prof: KmerProfile) {
  }

  case class CorrelationSums(n: Int, edgar: Double, cosine: Double, pearson: Double, commonCount: Double)

  @SerialVersionUID(39874)
  class Result(val alignment: Double, val edgar: Double, val cosine: Double, val pearson: Double, val commonCount: Double, val name1: String, val name2: String)
    extends Serializable

  def readDnaPacks(file: File, k: Int) : List[DnaPack] = {
    val seqs = FastaUtils.readFastaFile(file)
    val dnaPacks = seqs map {s => new DnaPack(s, KmerProfile(k)(s.content) )}
    dnaPacks
  }

  def printMissingAlignments(dnaPacks: Iterable[DnaPack], alignments: AlignmentCache) = {
    for (
      x <- dnaPacks; y <- dnaPacks;
      if alignments.get(x.seq.name, y.seq.name).isEmpty
    ) {
      println(x.seq.name, y.seq.name)
    }
  }

  def calcAlignments(dnaSeqs: List[DnaSeq]) = {
    val pairs = (for (
      x <- dnaSeqs; y <- dnaSeqs;
      if x.id < y.id
    ) yield (x, y)).toList

    val alignParams: AlignmentParameters = AlignmentParameters.usearch

    // Init progress bar
    val n = dnaSeqs.length.toLong
    val stepsSize = math.max(2,n / 1000L)
    val progress = new SyncProgress(n / stepsSize)

    // Calc proxies:
    progress.start()
    val results = pairs.par.map({case (x, y) =>
      // We need to hit each x id once
      // We know the seq ID's are consecutive integers 0,1,2,3,...
      // So only count the first of the x pairs where x.id = y.id - 1
      if (x.id % stepsSize == 0L && y.id - 1 == x.id) {
        progress.tick()
      }
      val align = JalignerUtils.calcIdentity(x, y, alignParams)
      (x.name, y.name, align)
    }).toList
    progress.end()
    results
  }

  def calcProxies(dnaPacks: List[DnaPack], alignments: AlignmentCache) : List[Result] = {
    val pairs = (for (
      x <- dnaPacks; y <- dnaPacks;
      if x.seq.id < y.seq.id;
      if alignments.get(x.seq.name, y.seq.name).isDefined
    ) yield (x, y)).toList
    val results = pairs.par.map({case (x, y) =>
      val align = alignments.get(x.seq.name, y.seq.name).getOrElse(-1.0)
      val edgar = x.prof.edgarKmerDistance(y.prof)
      val commonCount = x.prof.commonCountFrac(y.prof)
      val cosine = x.prof.cosineSimilarity(y.prof)
      val pearson = x.prof.pearson(y.prof)
      new Result(align, edgar, cosine, pearson, commonCount, x.seq.name, y.seq.name)
    }).toList
    results
  }

  def saveResults(results: List[Result], toConsole: Boolean) = {
    val outFile = new File("out.txt")
    val out = new java.io.PrintWriter(outFile)
    try {
      val line1 = "%s\t%s\t%s\t%s\t%s\t%s\t%s%n".format("Seq1", "Seq2", "alignment", "edgar", "cosine", "pearson", "commonCount")
      if (toConsole) print(line1)
      out.print(line1)
      results foreach { x =>
        val line = "%s\t%s\t%s\t%f\t%f\t%f\t%f%n".format(x.name1, x.name2, x.alignment, x.edgar, x.cosine, x.pearson, x.commonCount)
        if (toConsole) print(line)
        out.print(line)
        out.flush()
      }
    } finally {
      out.close();
    }
  }

  def serializeResults(results: List[Result], file: File) = {
    println(file)
    time("Serializing Results.") { () =>
      IO.serialize[List[Result]](file, results)
    }
  }

  def deserializeResults(file: File) = {
    println(file)
    time("Deserializing Resutls.") { () =>
      IO.deserialize[List[Result]](file)
    }
  }

  def evaluateFalsePositives(results: List[Result]) = {
    def printEvalLine(limit: Double) = {
      val truePos = results filter (x => x.alignment >= limit) toList
      val truePosCount = truePos.length;

      def calcFalsePositive(toProxy: Result => Double) = {
        val lAli = results map (_.alignment)
        val lEdg = results map (_.edgar)
        val lCos = results map (_.cosine)
        val lPea = results map (_.pearson)
        val lCC = results map (_.commonCount)

        val truePosProxy = truePos map toProxy;
        val truePosAllign = truePos map (_.alignment)

        val minProxy = truePos map (toProxy) min;
        val proxyPos = results map (toProxy) filter (_ >= minProxy) toList
        val proxyPosCount = proxyPos.length.toDouble;

        val falseNeg = 1.0 - truePosCount.toDouble / proxyPosCount
        falseNeg
      }
      print("%.1f\t%d\t%.4f\t%.4f\t%.4f\t%.4f%n".format(
        limit,
        truePosCount,
        calcFalsePositive(_.edgar),
        calcFalsePositive(_.cosine),
        calcFalsePositive(_.pearson),
        calcFalsePositive(_.commonCount)))
    }

    println("FALSE POSITIVE")
    printf("Total: %d%n", results.length)
    val line1 = "%s\t%s\t%s\t%s\t%s\t%s%n".format("Limit", "Matches", "edgar", "cosine", "pearson", "commonCount")
    print(line1)
    for (limit <- 100 to 70 by -1) {
      printEvalLine(limit)
    }
  }

//  def plotSeriesTo[T](name: String, series: Seq[PlotSeries[T]], data: List[T], rangeOpt: Option[PlotRange] = None, xlabel : String = "", ylabel : String = "") = {
//    val f = Figure()
//    val p = f.subplot(0)
//    p.setXAxisDecimalTickUnits()
//    p.setYAxisDecimalTickUnits()
//    if (rangeOpt.isDefined) {
//      val range = rangeOpt.get
//      p.xlim(range.xmin, range.xmax)
//      p.ylim(range.ymin, range.ymax)
//    }
//    series foreach { s =>
//      val xs = data map s.xf
//      val ys = data map s.yf
//      p += plot(xs, ys, s.draw, name = s.name)
//    }
//    p.xlabel = xlabel
//    p.ylabel = ylabel
//    p.legend = true
//    new File("fig/").mkdirs()
//    f.saveas("fig/" + name + ".png")
//  }

//  def plotHistTo[T](name: String, valf: T => Double, list: List[T], rangeOpt: Option[PlotRange] = None, log : Boolean = false) = {
//    val vals = list map valf
//    val numBins = 100
//    val binArray = Array.ofDim[Int](numBins)
//    val (min,max) = (vals.min, vals.max)
//    val range = max - min
//    vals foreach {v =>
//      val i = if (v == max) numBins - 1 else  ((v - min) * numBins / range).toInt
//      binArray(i) = binArray(i) + 1
//    }
//    val bins = binArray map {i : Int => if (log && i == 0) 0.1 else i.toDouble} toList
//    val binNames = (min to (max + range) by (range/100.0)).toList.take(100)
//
//    val f = Figure()
//    val p = f.subplot(0)
//    p.setXAxisDecimalTickUnits()
//    p.setYAxisDecimalTickUnits()
//    if (rangeOpt.isDefined) {
//      val range = rangeOpt.get
//      p.xlim(range.xmin, range.xmax)
//      p.ylim(range.ymin, range.ymax)
//    }
//
//    p += plot(binNames, bins,'.')
//    p.xlabel = name
//    p.ylabel = "count"
//    p.logScaleY = log
//    new File("fig/").mkdirs()
//    f.saveas("fig/" + name + ".png")
//  }

  class DataFiles(val genes: File, val align: File, val serial: File);
  def getFiles(args: ProxyArgs.ProxyCommand) : DataFiles = {
    getFiles(args.dataset(), args.k())
  }

  def getFiles(dataset: String, k : Int) = {
    dataset match {
      case x if x.matches("[0-9]+") =>
        new DataFiles(
          new File(geneBucketPath, dataset + ".fa"),
          new File(geneBucketPath, dataset + ".align"),
          new File(geneBucketPath, dataset + "-k%d.serial".format(k))
        )
      case x if x.toUpperCase.startsWith("HMP") =>
        val size = x.substring(3)
        val name = "HMP253.%s000".format(size)
        new DataFiles(
            new File(geneBucketPath, name + ".fa"),
            new File(geneBucketPath, name + ".align"),
            new File(geneBucketPath, name + "-k%d.serial".format(k))
        )
      case x => throw new IllegalArgumentException("No such dataset" + x)
    }
  }

  def runAlign(args: ProxyArgs.Align) = {
    println("Calculating proxies for k=" + args.k() + " and dataset = " + args.dataset())
    val files = getFiles(args);
    printFiles(files)
    val dnaSeqs = time("Loading Sequences.") { () =>
      FastaUtils.readFastaFile(files.genes)
    }
    val alignments = time("Calculating Alignments") {() =>
      calcAlignments(dnaSeqs)
    }
    time("Saving Alignments"){() =>
      IO.printToFile(files.align){w =>
        alignments.foreach{case (x,y,a) => w.println("%s\t%s\t%f".format(x,y,a*100.0))}
      }
    }
  }

  def runCalcProxy(args: ProxyArgs.CalcProxy) = {
    println("Calculating proxies for k=" + args.k() + " and dataset = " + args.dataset())
    val files = getFiles(args);
    printFiles(files)

    val alignments = time("Loading Alignments.") { () =>
      AlignmentCache(files.align)
    }
    val dnaPacks = time("Loading Sequences.") { () =>
      readDnaPacks(files.genes, args.k())
    }
    val results = time("Calculating Proxies.") { () =>
      calcProxies(dnaPacks, alignments)
    }
    serializeResults(results, files.serial)
  }

  def printFiles(files: DataFiles) = {
    println(files.align)
    println(files.genes)
    println(files.serial)
  }

  def runPlot(args: ProxyArgs.Plot) = {
    println("Creating plots for k=" + args.k() + " and dataset = " + args.dataset())
    val files = getFiles(args);
    printFiles(files)
    val results = deserializeResults(files.serial)

    val aligF = { x: Result => x.alignment }
    val edgaF = { x: Result => x.edgar }
    val cosiF = { x: Result => x.cosine }
    val pearF = { x: Result => x.pearson }
    val commF = { x: Result => x.commonCount }

    time("Plotting"){() =>
      val plotResults = results filter (_.alignment > args.minAlign())
      val prefix = "fig/%s_k%d_".format(args.dataset(),args.k())
      def plotTo(name : String, f : Result => Double): Unit = {
        val plot = scatterPlot("alignment", name)
        val data = plotResults map (r => (r.alignment, f(r)))
        plot += points(data, "")
        plot.save(new File("%s%s.png".format(prefix, name)));
      }
      plotTo("Edgar", edgaF)
      plotTo("Cosine", cosiF)
      plotTo("Pearson", pearF)
      plotTo("CommonCount", commF)
    }
  }


  def runStats(args: ProxyArgs.stats) = {
    println("Creating statistical plots for k=" + args.k() + " and dataset = " + args.dataset())
    val files = getFiles(args);
    printFiles(files)
    val results = deserializeResults(files.serial)
    val dnaPacks = time("Loading Sequences.") { () =>
      readDnaPacks(files.genes, args.k())
    }
    val dir = "fig/"
    new File(dir).mkdirs()
    val prefix = dir + "stats_" + args.dataset.get.get + "_" + args.k.get.get + "_"

    // Functions
    val condition = {r : Result => r.alignment >= 95.0}

    // Data
    val lengths = dnaPacks map {p => p.seq.content.length.toDouble}
    val cutoffs = 0.0 to 1.0 by 0.005
    val statsData = time("Calc stat data") {() =>
      (for (y <- cutoffs) yield {
        ClassificationStats(results)(condition, {_.cosine >= y})
      }).toList
    }
    val data = statsData zip cutoffs toList

    // Plot
    time("length") {() =>
      val plot = histPlot("length")
      plot += hist(lengths)
      plot.save(new File(dir + args.dataset() + "_length.png"));
    }
    time("precision-recall") {() =>
      val plot = linePlot("cutoff", "")
      plot += points(data map (d => (d._2, d._1.precision)), "precision")
      plot += points(data map (d => (d._2, d._1.recall)), "recall")
      plot.save(new File(prefix + "precision-recall.png"));
    }
    val posResults = results filter condition
    val negResults = results filter condition.andThen(!_)

    def plotHist(name: String, f : Result => Double, range : NumericRange[Double] ) = {
      time(name) {() =>
        val posHist = histRange(posResults map f, "positives", range)
        val negHist = histRange(negResults map f, "negatives", range)

        val plot = histPlot(name)
        plot += posHist
        plot += negHist
        plot.save(new File(prefix + name + ".png"));

        val logPlot = histPlot(name).log()
        logPlot += posHist
        logPlot += negHist
        logPlot.save(new File(prefix + name + "Log.png"));
      }
    }
    plotHist("alignment", {r => r.alignment}, 0.0 to 100.0 by 1.0)
    plotHist("cosine", {r => r.cosine}, 0.0 to 1.0 by 0.01)
  }

  def runKstats(args: kstats): Unit = {
    println("Creating statistical plots for k=%d-%d and dataset = %s".format(args.minK(), args.maxK(), args.dataset()))
    val ks = args.minK() to args.maxK()

    val means = (for (k <- ks) yield {
      val files = getFiles(args.dataset(), k)
      val results = deserializeResults(files.serial)

      time("Summing Correlations"){() =>
        val n = results.length
        var cc = 0.0
        var cos = 0.0
        var edgar = 0.0
        var pearson = 0.0
        results.foreach{r =>
          cc = cc + r.commonCount
          cos = cos + r.cosine
          edgar = edgar + r.edgar
          pearson = pearson + r.pearson
        }
        CorrelationSums(n,edgar, cos, pearson, cc)
      }
    }).toList

    val dir = "fig/"
    new File(dir).mkdirs()
    val prefix = dir + "k_" + args.dataset() +  "_"

    def plotKStats(fileName : String, legend : String, f : CorrelationSums => Double) = {
      val dks = ks.map(_.toDouble)
      val crossIndex = 7
      val crossK = ks(crossIndex)

      def plotWithReference(vals : Seq[Double], name : String, yLabel : String) = {
        // Create reference lines
        val crossVal = vals(crossIndex)
        val half = dks.map(k => crossVal / math.pow(2,k-crossK))
        val quarter = dks.map(k => crossVal / math.pow(4,k-crossK))

        val plot = linePlot("k", yLabel)
        plot += points(dks, half, "½ᵏ")
        plot += points(dks, quarter, "¼ᵏ")
        plot += points(dks, vals, legend)
        plot.save(name + ".png")
        plot.log().save(name + "_log.png")
      }
      plotWithReference(means.map(f), prefix + fileName, "Sum of correlations")
      plotWithReference(means.map(x => f(x)/x.n), prefix + fileName + "avg", "Average correlation")
    }

    time("Plotting"){() =>
      plotKStats("cosine", "Cosine Similarity", _.cosine)
      plotKStats("commonCount", "Common Count", _.commonCount)
      plotKStats("edgar", "Edgar Similarity", _.edgar)
      plotKStats("pearson", "Pearson Correlation", _.pearson)
      // plotKStats("n", "Number of pairs", _.n)
    }
  }

  def main(argStrings: Array[String]) {
    val isEclipse = sys.env.get("ECLIPSE_RUN").map(_.toBoolean).getOrElse(false)
    val argStrings2 = if (isEclipse) Seq("plot", "-d", "210", "-k", "8") else argStrings.toSeq
    val args = new ProxyArgs(argStrings2)

    args.subcommand match {
      case Some(args.align) => runAlign(args.align)
      case Some(args.calcProxy) => runCalcProxy(args.calcProxy)
      case Some(args.plot) => runPlot(args.plot)
      case Some(args.stats) => runStats(args.stats)
      case Some(args.kstats) => runKstats(args.kstats)
      case Some(x) => args.errorMessageHandler("Unknown command: " + x)
      case None => args.errorMessageHandler("Please specify a command.")
    }
  }
}
