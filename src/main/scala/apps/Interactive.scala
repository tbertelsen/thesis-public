package apps

import java.util.Random

import java.io.File
import apps.Timer._
import breeze.linalg.*
import org.apache.spark.graphx.Graph
import sequences.{KmerProfile, DnaSeq}
import sequences.jaligner.AlignmentParameters

import scala.io.Source

/**
 * Created by tobber on 20/3/15.
 */
class Interactive {

  /*
import apps.SparkClustering._
import geneio.{FastaSpark, UClustLine}
import apps.SimpleSpark
import sequences.{KmerProfile, ClusterRdd}

   */


  def posLong(): Long = {
    val long = new Random().nextLong()
    if (long < 0) {
      -(long / 4)
    } else {
      long / 4
    }
  }

  {
    println ()
    println ("@SerialVersionUID(%dL)".format (posLong ()))
    println ("@SerialVersionUID(%dL)".format (posLong ()))
    println ("@SerialVersionUID(%dL)".format (posLong ()))
    println ("@SerialVersionUID(%dL)".format (posLong ()))
    println ("@SerialVersionUID(%dL)".format (posLong ()))
    println ("@SerialVersionUID(%dL)".format (posLong ()))
  }


  val ucfile = new File("1495.uc")
  val fafile = new File(SimpleSpark.fa1495)
  import apps._
  import splot._
  import geneio._
  import sequences._
  import utils._
  def stats(clust:ClusterRdd) = {
    clust.cache()
    println()
    println(clust.rdd.count())
    println(clust.rdd.map(c => 1 + c.members.length).reduce(_ + _))
    println(clust.calcAverageInformation(1.0, 0.95))
    println(clust.calcAverageInformation(0.5, 0.95))
    println(clust.calcAverageInformation(2.0, 0.95))
  }
  val sc = SimpleSpark.startSparkLocal(4)
  def testClust(moreArgs: String) : ClusterRdd = {
    val args = new GeneClusteringArgs(("benchmark -f %s -o tests -m FAKE".format(fafile.toString) + moreArgs).split(" ")).benchmark
    Timer.time("Clusters: " + moreArgs) { () =>
      val clust = GeneClustering.doCluster (args, sc)
      stats (clust)
      clust
    }
  }
  def testCorr(moreArgs: String) = {
    val args = new GeneClusteringArgs(("benchmark -f %s -o tests -m FAKE".format(fafile.toString) + moreArgs).split(" ")).benchmark
    Timer.time("Correlates: " + moreArgs) { () =>
      val sparkCorrelation = new SparkCorrelation(args.tuning())
      val correlationGraph = sparkCorrelation.createCorrelationGraph(sc, args.inputFile())
      if (args.findProxyCenters()) {
        correlationGraph.edges.count()
        correlationGraph
      } else {
        val identityGraph = GeneClustering.toIdentityGraph(correlationGraph, args)
        identityGraph.edges.mapValues(_ => 1).count()
        identityGraph
      }
    }
  }




  testClust("")
  testClust(" -a 1.0 --proxy-centers --exact-corr")
  testClust(" -a 1.0")
  testCorr(" -a 1.0 --proxy-centers --exact-corr")
  testCorr(" -a 1.0")
  testClust(" -a 0.5 --proxy-centers --exact-corr")
  testClust(" -a 2.0 --proxy-centers --exact-corr")



  // Test Approximate Correlation:
  val args = new GeneClusteringArgs("benchmark -f %s -o tests -m FAKE -l 60".format(fafile.toString).split(" ")).benchmark
  val tuning = args.tuning()
  val file = fafile
  val sparkCorrelation = new SparkCorrelation(args.tuning())

  val simpleFileName = file.getName
  val genes = FastaSpark.readFastaFile(sc, file.toURI).setName("Genes (%s)".format(simpleFileName)).cache()
  val k = tuning.k
  val toKmer = KmerProfile(k)_
  val kmers = genes map {dna => (dna.id, toKmer(dna.content).normalize)} setName("Kmers (%s)".format(simpleFileName)) cache()
  val cutOff = tuning.approxCutoff;
  val samplePairs = sparkCorrelation.sampleCorrelationKmer(kmers)
  val kmerPairs = SimpleSpark.pairLookup(samplePairs.map(_._1), kmers)
  val pairs = samplePairs.join(kmerPairs)
  val actualAndApproxCorr = pairs.map{case (ids,(approx,(kmer1,kmer2))) => (kmer1.cosineSimilarity(kmer2), approx)}
  val data = actualAndApproxCorr.filter(_._1 > 0.1).collect()

  val plot = scatterPlot("actual","approx")
  plot += points(data, "")
  plot += function(x => x, name = "y=x")
  plot.save("test2.png")



  val dna = FastaUtils.readFastaFile(fafile)
  val kmer = KmerProfile(14)(dna.head.content)
  val vector = kmer.profile


  val norm1 = breeze.linalg.normalize(vector)
  val norm2 = breeze.Utils.normalize(vector)
  norm1.activeSize
  norm2.activeSize


  val lines = Source.fromFile(ucfile).getLines();
  val line1 = new UClustLine(lines.next())
  val line2 = new UClustLine(lines.next())
  val line3 = new UClustLine(lines.next())

}

object InteractiveTuning {

  /*

import java.io.File
import apps.{TuneTester, SimpleSpark}

*/

  val input = new File(SimpleSpark.fa210)
  val wordDir = new File("tuneTests/", input.getName)
  val master = "local[4]"

  TuneTester.main(Array("count", "-w", wordDir.getAbsolutePath, "-i", input.getAbsolutePath))
  TuneTester.main(Array("count", "-p", "-w", wordDir.getAbsolutePath, "-i", input.getAbsolutePath))
  TuneTester.main(Array("run", "-w", wordDir.getAbsolutePath, "-i", input.getAbsolutePath, "-m", master))
  TuneTester.main(Array("plot","-o", "-w", wordDir.getAbsolutePath))



  import org.apache.spark.graphx.Edge
  import org.apache.spark.graphx.Graph
  val sc = apps.SimpleSpark.startSpark("local[4]", "Test Tuning")

  def printEdgesCount(graph : Graph[sequences.DnaSeq, Double]) = {
    val count = graph.edges.count()
    val canonCount = graph.convertToCanonicalEdges{(a,b) => a}.edges.count()
    val groupCount = graph.groupEdges{(a,b) => a}.edges.count
    println("%d  %d  %d".format(count, canonCount, groupCount))
  }

  def testRounds(rounds: Int, newAlgo : Boolean = false) = {
    val tuning = new apps.Tuning(k = 14, exactCutoff = 0.35, calcExact = false, lambda = 40, approxCutoff = 0.25, expandedSearchRounds = rounds, newExpandAlgorithm = newAlgo)
    val engine = new apps.SparkCorrelation(tuning)
    val proxyGraph = engine.createCorrelationGraph(sc, new File(apps.SimpleSpark.fa210).toURI)
    val idGraph = apps.GeneClustering.toIdentityGraph(proxyGraph, 0.95, sequences.jaligner.AlignmentParameters.usearch)
    printEdgesCount(proxyGraph)
//    printEdgesCount(idGraph)
  }

  testRounds(0)
  testRounds(1, false)
  testRounds(2, false)
  testRounds(1, true)
  testRounds(2, true)



}

object InteractiveKmer {


  def small: Unit = {
    val genes = List(
      "ATGGAAATGA",
      "ATGGAAAATG",
      "GTGCCCGCCC",
      "ATGCTCAGTC",
      "TTGCCCCCTG",
      "ATGTTAAATT"
    )
    val kmers =  List ("AA", "AC",  "AG", "AT",  "CA", "CC")
    printFigs(genes, kmers)
  }


  def medium(): Unit = {
    val genes = List (
      "GTGGCTAAAAAAGAGGGCGCTGCCTCCACGGGCGTCGAAGGCGGCGCGGTCGATGCGAGTGCGCTCACCGAAGACGCTCT",
      "ATGACTAGCAACTTAGAAGCTCAACTTTTAGCACTGCGGCAGGAAGGAGAACAGGCGATCGCCGCCGCCGATACCCTCGA",
      "ATGCAAGAAGCATTAGCAGAGATAGTCGCAAAGGCTCGTCGGGCGGTGTCAGCCGCCCCTGATTTGCCAACCTTGGATGA",
      "GTGCCCGCCCCTGGAGAGACTGTCGACCCGTCCCGGGTCCACGCCCTCGACCCCGATCAGCTCGACGCGGCCGTCAACGC",
      "ATGTTAAATTTAAATAAATTATTTGAAATTATAGAAATAGATATACAAAATTCAAAAAAAATTTCCGAACTAGATAAAAT",
      "GTGAGCGCACCGGTCACCCTGCAGCAGCTCACCGATCAACTCGATGCCCTCGAGCAGCAGGCCGCGGCGGAGATCGCCGA",
      "ATGCTCAGTCAGCTCAGCGAGCTTGAGGCTCAACTCGAAGCTCTGCAAACCCGTGCTCTGGAAGCGATCGCCACAGCGAG",
      "GTGTCCGACCTCGCCTCGCTCGAAACCTCGATCCTCGACCAGATCGCCGCCGCTGGCGACGAAGCCGCGCTGGAAGCGGT",
      "ATGACTATTTCTTTGGAAGCCGATTTAAAATCCCTGCAACAGTCTGCCCAGGCGGCGATCAGCGGTTGTGACGACCTGGA",
      "ATGGAAATGAAAGAAGAGATTGAAGCTGTAAAGCAGCAATTTCACTCTGAGTTAGATCAGGTGAACTCTTCTCAGGCACT",
      "ATGCAAATAGTCGAGCAAATGAAAGATAAAGCTCTTGCTGAGCTAAATCTTGTCAAAGATAAAAAAACTTTAGATGATAT",
      "ATGGAAAATGTAAACCGCATCGTTGCAGAAGGCATTGCCGCAGTAGAAGCTGCGCAAGACTTCAACGCTCTAGAACAAAT",
      "TTGGGGCTTAAAGAGGATCTGAAAGCTTTAGAAGAAGAGGCTAAAAAACTTATAGAAGAAGCATCAGATATAGGCAAACT",
      "ATGGAGATAGAAGCCGTCGAAAAGGAAGCCATCGAGAAATTATCGAAGATATCGAACGTTCAGGAGCTGGAAAGTTTCAG",
      "ATGCTCGACGCGCTGAAAGACGAACTTCTGTCGCAGGTCAACGCCGCCGCCGACCTCTCGGCGCTGGACGAGGTGCGGGT",
      "TTGAAACAGGAAGTTCATCGTATTCAAGAAGAAACACTGGCAGAGCTTCAGCAGGTCTCGACTTTGGAAGCGCTGCAGGA",
      "GTGATACTTATTTTTACTATATCAGTGAGTCAAATTGAATCATTAAGTCAAATTGAGGGAAAACTAAATAATCTTTCTCT",
      "TTGCCCCCTGCGTTTCGAGCCCCCGGCGCCGCGTCGCTCTCTGTGACCCTTGACTCGCGATTCCCTTCCATGCCCGACGA",
      "ATGCAACATCTAGAAGAGATCATTGCTAGCGCGAGCACTGCAATTGAAGCTGCCGAATCGCTAGTCGCACTTGATGAAGT"
    )

    val kmers = List ("AAAA", "AAAC", "AAAG", "AAAT", "AACA", "AACC", "AACG", "AACT", "AAGA", "AAGC", "AAGG", "AAGT",
      "AATA", "AATC", "AATG", "AATT", "ACAA")

    printFigs(genes, kmers)
  }


  def large(): Unit = {
    val genes = List("ATGGAAAACCTGGACGCGCTCGTTGCTCAAGCCCTTGAGGCAGTGCAAAGCGCTGAAGATGTAAATGCCCTGGAGCAAAT",
      "ATGTCAGATATCGACCAGCTCAACACATCGCTGCTCGCCGAAATCGCTGCCGCCGATGACGAGACGGCGCTCGAAGCCGT",
      "ATGCAACATCTACAAGAGATCATTGCTAACGCGAATGCTGCGATTGATGCCGCACAGTCGCTTGTCGCACTCGATGAAGT",
      "ATGCCACATCTCGCAGAGCTGGTTGCCAAAGCCAGAACAGCTATAGAAGAGGCCCAGGATGTTGCCGCACTGGAAAATGT",
      "ATGTCAAACATTGAACAACAGTTGGCTGAATTAAGCCAAACGACACTAGAAAAGTTAAAAGAAATCCAGCACCAAGGAGA",
      "ATGGATTTACAACAACAATTAGAAGAACTGAAACAGCAAACACTTGAACACTTAACATCATTAACTGGTGATCATAGTAA",
      "GTGGATGAAATACAGGACATAGAGGGTCTGACACGCCGTGCGCTTCAGGCAATCTCCCGTATTGACACTCTTCGTGATTT",
      "ATGGCGATTCAAGAGGAGCTTGAAGCTACAAAGCAACAATTTTGTGCAGAACTCAGTCAAGTTAACTCTTCAAAAGATCT",
      "ATGTCACATCTCGCAGAGCTGGTTGCCAATGCAGCGGCCGCCATTAACCAGGCGTCAGATGTTGCCGCGTTAGACAATGT",
      "ATGGAAAACCTGGATGCGCTGGTTTCTCAAGCACTTGAGGCCGTGCAACACGCTGAAGACATCAATGCCCTGGAACAGAT",
      "ATGAGTTTACAAGATCGATTAACCGAATTACGCGATCAAGGCTTGGCCGATATTAAATCCGCCGATGTTTTGAAAAAGGT",
      "ATGAAAGAAGAATTACACGCCATCCGTGATGAGGCAATTGCTTCTGTTGAAGAAGCAAAAGATATGAAATCATTACAGGA",
      "ATGCAAAACCTAAAAGAAATTACGGAACAAGCCCGTGCAGCTCTGGACGAATTGCATGATAAAGGGTTAGACGCATTAGA",
      "ATGGAAGCACGTTTAAAAGAGCTAAAGCAAAAAGCGTTAGAGCTTATTGAAGAAGCGAAAGAGCTAAAAGGCTTAAACGA",
      "GTGACTGTTAATTATGAGAAACTCAGAAGGCGTTTTTATTTACGTCTTGACGCTGTGTTTTCTGTTGAGGAGCTTAACAA",
      "ATGGAAAACCTGGACGCGCTGGTCTCTCAAGCTCTTGAGGCTGTGCAAAGCGCCGAAGATATCAATGCCCTGGAGCAAAT",
      "ATGCAACATCTAGAAGAGATCATTGCTAGCGCGAGCACTGCAATTGAAGCTGCCGAATCGCTAGTCGCACTTGATGAAGT",
      "ATGGAAGAAAAGCTAAAACAGCTGGAACAAGAAGCTTTAGAACAAGTAGAAGCGGCAAGCTCATTGAAGGTTGTCAATGA",
      "ATGGAAAAGCTCGACAAAATCCTCGAGGAGTTAAAATTACTCCTCTCTTCTGTCTCTTCCCTTAAAGAACTTCAAGAAGT",
      "ATGCTTTCAAAAATTGAACTCATGCTGCTCCGTTCTCTCGAAACCGGGAAGACCTACACACCTGAGGAGGCTGCTGAGCT",
      "TTGAAGGTTATATTTATGAAGGCAGATTTAAATTTAATAAAAACATTGCACCCTCTTGAGATAAAAGTAATTGTAAATAA",
      "GTGTCCGACCTCGCAACGCTCGAAACATCCATCCTCGATCAGGTCGCCGCCGCCGGCGACGAAGCCGCCCTCGAAGCGGT",
      "ATGAGCGATCTAGAACAACTCGAACGCCAGATTCTCGAAGACATCGCCGCGGCAGTGGATGAGCAGGGTATTGAGGCCGT",
      "ATGTGTGATGCGCTAAAATCTATTAAAAAAATTAAAAAAGAAATTCAACGAACTACTACTGTTGAGGAATTAAAGACATT",
      "ATGATGTCACAGTTAACAGAGATCGTAGAACAGGCCTTAGCCGCCATCAAAGACGCTACAGATTTAAAAGCGCTTGATGA",
      "ATGACATTACAAGCGCAATTAGAAGCTCTTAGAGACAATACGCTCAAAGAAATCGCACAAGTTGCTACTTTAAAAGAATT",
      "ATGATAGCTAAGATTGAACAACTTCTGAAAGAGGTGGAAGCTTTGCACGCCTCCAATGCCGAAGAACTCGAAGCTCTCCG",
      "ATGGAAGCACGTTTAAAAGAGCTAAAGCAAAAAGCGTTAGAGCTTATTGAAGAAGCGAAAGAGCTGAAAGGCTTAAACGA",
      "ATGCAGAACCAATTAAACGCACTTTTACAATCTGCAAAAAAATCCGTTGCCGACGCTCAAAGCGAAATTGTGCTCGAAGA",
      "TTGAAACCGGAAGAGATCGAGCGCATGCGGGACGAGGCGCTCGCCGCCTTCGCGGCCGCCGGCGACCTCGACGCGCTCCA",
      "ATGATGATTCAAGAGGAGCTTGAAGCTACAAAGCAACAATTTTGTATTGAGTTAGATCAAGTTCACTCTTCAAAAGATCT",
      "ATGTATAACCTACAAGAAATTACTGAAGAAGCACGGAAAGCAATTGAGGCACTGCAAGATAAGAGCATTGAAAGCTTGGA",
      "GTGTCCGACATAACCGCGATCGAACAAGAAGCGCTTGCCGCTATCGCTGCTGCCGCCGATCTTGCCGCGCTCGACGCCGT")

    val kmers = List("AAAAA", "AAAAC", "AAAAG", "AAAAT", "AAACA", "AAACC", "AAACG", "AAACT", "AAAGA", "AAAGC", "AAAGG", "AAAGT", "AAATA", "AAATC", "AAATG", "AAATT", "AACAA", "AACAC", "AACAG", "AACAT", "AACCA", "AACCC", "AACCG", "AACCT", "AACGA", "AACGC", "AACGG", "AACGT", "AACTA", "AACTC", "AACTG", "AACTT", "AAGAA")
    printFigs(genes, kmers, "lg")
  }


  def printFigs(genes : List[String], kmers: List[String], name: String = "") = {
    val k = kmers.head.length
    val profiles = genes.map{g => sequences.DnaSeqBits(g)}.map{s => sequences.KmerProfile.apply(k)(s)}

    def grid(fill : () => Unit): Unit = {
      println()
      println("\\begin{tikzpicture}")
      println("  \\def\\rows{%d}".format(genes.length))
      println("  \\def\\cols{%d}".format(kmers.length))
      println(
        """
          |  % Make text match cell size
          |  \tikzstyle{every node}=[font=\Huge]
          |
          |  % Labels
          |  \foreach \r in {1,...,\rows} {
          |    \rowlabel{\r}{G\r};
          |  }
        """.stripMargin)
      kmers.zipWithIndex.foreach{case (s,i) => printf("  \\collabel{%d}{%s}%n",i+1,s)}
      fill()
      println()
      println("  % Cells")
      println(
        """
          |
          |  % Grid
          |  \draw[step=1cm] (0,0) grid (\cols,\rows);
          |  \dotpad{\cols}{\rows}
          |
        """.stripMargin)
      println("\\end{tikzpicture}")
    }
    grid{() =>
      for ((p,i) <- profiles.zipWithIndex) {
        for ((k,j) <- kmers.zipWithIndex) {
          val c = p(j)
          if (c > 0.0) {
            println("  \\ccell{%d}{%d}{%.0f}".format(j+1,i+1,math.min(c*10,100)))
          }
        }
      }
    }
    grid{() =>
      for ((p,i) <- profiles.zipWithIndex) {
        for ((k,j) <- kmers.zipWithIndex) {
          val c = p(j)
          if (c > 0.0) {
            println("  \\celltext{%d}{%d}{%.0f}".format(j+1,i+1,c))
          }
        }
      }
    }

    println("\\begin{tikzpicture}")
    println(
      """
        |  % Make text match cell size
        |  \tikzstyle{every node}=[font=\Huge]
        |
      """.stripMargin)
    for ((g,i) <- genes.zipWithIndex) {
      printf("  \\rowlabel{%d}{%3s}%n",i+1,"G" + (i+1))
      printf("  \\rowdna{%d}{%s}%n",i+1,g)
    }
    println("\\end{tikzpicture}")
  }
}
