package apps

import java.io.File

import breeze.linalg.*

/**/
import geneio.FastaUtils


/**
 * Created by tobber on 4/3/15.
 */
object Subsampler {

  def main(args: Array[String]) {
    println("Started")
    val short = false
    val inMnt = !short && new File("/mnt/eva/").exists

    val sizeFactor = if (short) 100 else 1000
//    val sizes = List(2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384).map(_ * sizeFactor)
    val sizes = List(2048).map(_ * sizeFactor)

    val dataPrefix = if (inMnt) "/mnt" else ""
    val dataDir = if (short) "/eva/users/dkn957/data/" else  dataPrefix + "/eva/projects/smash/MOCAT/human/stool/US_HMP/"
    val dataName = if (short) "1495" else "HMP253.genes.nucleotide"
    val dataPath = dataDir + dataName + ".fa"

    val inputFile = new File(dataPath)
    val outputDir = dataDir

    println("Counting lines " + inputFile.getAbsolutePath)
    val inputTemp = FastaUtils.readFastaFileStrings(inputFile)
    var count = 0L
    println()
    inputTemp.foreach{s =>
      if (count % 10000L == 0L) {
        print("\r%,15d".format(count))
      }
      count = count + 1L
    }
    println("\r%,15d".format(count))
    val n = count

    val input = FastaUtils.readFastaFileStrings(inputFile)

    val probs = sizes map {_ / n.toDouble}

    val outputFiles = sizes map (s => outputDir + "%s.%d.fa".format(dataName, s))
    val outputWriters = outputFiles map {s => new java.io.PrintWriter(new File(s))}
    val probsAndWriters = probs zip outputWriters

    println("Subsampling " + inputFile.getAbsolutePath + " to ")
    outputFiles zip probs foreach(t => println(t._1 + " \t" + t._2))

    println()
    try {
      val prog = new Progress(n).start()
      input.foreach{(fastaString : String) =>
        prog.tick()
        probsAndWriters.foreach { case (prob, writer) =>
          if (prob > math.random) {
            writer.print(">")
            writer.print(fastaString)
            writer.print("\n")
          }
        }
      }
      prog.end()
    } finally {
      outputWriters.foreach(_.close())
    }
  }

}

class Progress(val n : Long) {
  @volatile var progress = 0L
  @volatile var percent = 0L

  def start() = {
    println()
    print("  0 %")
    this
  }


  def tick(ticks : Int) : this.type = {
    tick(ticks.toLong)
  }

  def tick(ticks : Long = 1L) : this.type = {
    progress = progress + ticks
    if (progress > n / 100.0 * (percent + 1L)) {
      percent = progress * 100L / n
      print('\r')
      print("%3d %%".format(percent))
    }
    this
  }

  def end() = {
    print('\r')
    println("100 %")
    this
  }
}

class SyncProgress(n : Long) extends Progress(n) {
  override def tick(ticks : Long = 1L) : this.type = {
    this.synchronized{
      super.tick(ticks)
    }
    this
  }
}
