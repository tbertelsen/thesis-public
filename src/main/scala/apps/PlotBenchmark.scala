package apps

import java.io.File

import utils.IO.deserialize

/**
 * Created by tobber on 22/6/15.
 */
class PlotBenchmark(data: Vector[BenchmarkResults], outdir: File) {
  val filenameToDataSize = (filename: String) => {
    val r = """^HMP253\.([0-9]+)\.fa$""".r
    filename match {
      case r(group) => group.toDouble
      case _ => 32000000.0
    }
  }
  def createPlot(name: String, seriesFun: Meta => String, xFun: Meta => Double, xLabel: String, yFun: Vector[Double] => Double) = {
    val dataMap = data.groupBy(r => seriesFun(r.meta))

    def savePlot(dataType: String, dataFun: BenchmarkResults => Vector[Double], trendLines: Boolean) = {

      val plot = splot.linePlot(xLabel, dataType)
      for ((k,v) <- dataMap) {
        val seriesData = v.map (r => (xFun (r.meta), yFun (dataFun (r)))).sorted
        plot += splot.points (seriesData, k)

        if (trendLines) {
          val crossPoint = seriesData (seriesData.length - 2)
          val xs = seriesData.map (_._1)
          val linear = (x: Double) => crossPoint._2 * x / crossPoint._1
          val squared = (x: Double) => crossPoint._2 * math.pow (x / crossPoint._1, 2)
          plot += splot.points (xs, xs.map (linear), "O(n)")
          plot += splot.points (xs, xs.map (squared), "O(n²)")
        }
      }

      plot.save(new File(outdir, name + "_" + dataType + ".png"))
      plot.log().save(new File(outdir, name + "_" + dataType + "_log.png"))
      plot.logLog().save(new File(outdir, name + "_" + dataType + "_loglog.png"))
    }

    savePlot("Seconds", _.seconds, false)
    savePlot("SecondsTrends", _.seconds, true)
    savePlot("IIQ", _.avgInformation, false)

  }

  def createCorePlots() = {
    createPlot("Cores", _.filename, _.cores, "Cores", _.head)
  }

  def createDataSizePlots() = {
    createPlot("DataSize", _.cores + " cores", m => filenameToDataSize(m.filename), "Number of Genes", _.head)
  }
}

object PlotBenchmark {
  def main (args: Array[String]) {
    val datadir = new File(args.head)
    val datafiles = datadir.listFiles().filter(_.getName.endsWith(".bench")).toVector
    val data = datafiles.map(deserialize[BenchmarkResults](_))
    val plotter = new PlotBenchmark(data, new File(datadir, "fig"))
    plotter.createDataSizePlots()
  }
}
