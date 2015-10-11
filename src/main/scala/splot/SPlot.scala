package splot

import java.awt.geom.Line2D
import java.io.{FileOutputStream, File}

import org.jfree.chart.axis.{LogarithmicAxis, NumberAxis}
import org.jfree.chart.labels.StandardXYToolTipGenerator
import org.jfree.chart.renderer.xy.{AbstractXYItemRenderer, XYLineAndShapeRenderer, XYItemRenderer}
import org.jfree.chart.urls.StandardXYURLGenerator
import org.jfree.chart.{JFreeChart, ChartFactory}
import org.jfree.chart.plot.{XYPlot, PlotOrientation}
import org.jfree.data.xy.{XYDataset, XYSeries, XYSeriesCollection}
import javax.imageio.ImageIO;

/**
 * Created by tobber on 5/3/15.
 */
class SPlot(plotLines : Boolean, plotItems : Boolean, xLabel : String = "", yLabel : String = "")
    extends SAbstractPlot[XYSeries](xLabel, yLabel) {
  private val dataset = new XYSeriesCollection()

  def += (series : XYSeries) {
    dataset.addSeries(series)
  }

  override protected def getDataset() = dataset
  override protected def createRenderer(imageScale : Double) = {
    val r = new XYLineAndShapeRenderer(plotLines, plotItems)
    val w = 30.0 * imageScale
    r.setLegendLine(new Line2D.Double(-w, 0.0, w, 0.0))
    r
  }
}
