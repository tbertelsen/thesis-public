package apps

import java.io.File
import java.net.URI

import geneio.FastaSpark
import org.rogach.scallop._
import sequences.ClusterRdd
import sequences.jaligner.AlignmentParameters
import utils.IO
import utils.IO._

/**
 * Created by tobber on 21/7/15.
 */

class IiqCalculatorArgs(stringArgs: Seq[String])  extends ScallopConf(stringArgs) {
  printedName = "IiqCalculator"
  val cdhitFile = opt[URI](required = false, short = 'c',
    descr = "The file containing cluster assignments from cdhit")(singleArgConverter(fileOrURI(_)))
  val uclustFile = opt[URI](required = false, short = 'u',
    descr = "The file containing cluster assignments from uclust")(singleArgConverter(fileOrURI(_)))
  val fastaFile = opt[URI](required = true, short = 'f',
    descr = "The file containing genes in fasta format")(singleArgConverter(fileOrURI(_)))
  val outputFile = opt[File](required = true, short = 'o',
    descr = "Output file to store IIQ values in")(singleArgConverter[File](s => new File(s)))
  val master = opt[String](required = true, short = 'm',
    descr = "The URL to the spark master.")
  val alpha = opt[Double](default = Some(1.0), validate = a => a > 0.01 && 1 < 100,
    descr = "The exponent of the information function.")

  ConfHelp.defaultTweak(this)
}

object IiqCalculator {
  def main(argsStrings: Array[String]) {
    val args = new IiqCalculatorArgs(argsStrings)
    val app = new IiqCalculator(args)
    app.run
  }
}

class IiqCalculator(args: IiqCalculatorArgs) {
  private val alignParameters = AlignmentParameters.usearch

  def storeIiq(name: String, d: Double) = {
    val line = name + "=%.8f".format(d)
    println(line)
    IO.printToFile(args.outputFile(), true)(_.println(line))
  }

  def run: Unit = {
    IO.printToFile(args.outputFile())(_.print(""))

    val sc = SimpleSpark.startSpark(args.master(), "Iiq Calculation on " + fileNameFromURI(args.fastaFile()))
    val genes = FastaSpark.readFastaFile(sc, args.fastaFile()).persist(SimpleSpark.mediumCache)
    if (args.uclustFile.isDefined) {
      val clusters = ClusterRdd.fromUclustOutput(sc, args.uclustFile(), genes, alignParameters)
      val iiq = clusters.calcAverageInformation(args.alpha(), 0.95)
      storeIiq("uclust", iiq)
    }
    if (args.cdhitFile.isDefined) {
      val clusters = ClusterRdd.fromCdhitOutput(sc, args.cdhitFile(), genes, alignParameters)
      val iiq = clusters.calcAverageInformation(args.alpha(), 0.95)
      storeIiq("cdhit", iiq)
    }
    sc.stop()
  }
}
