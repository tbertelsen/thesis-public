package splot

import java.awt.geom.Rectangle2D

import breeze.linalg.*
import com.sun.org.apache.bcel.internal.generic.NEW
import org.jfree.chart.renderer.xy.{StandardXYBarPainter, XYBarRenderer, XYItemRenderer}
import org.jfree.data.statistics.HistogramDataset
import org.jfree.data.xy.{XYDataset, XYSeriesCollection, XYSeries}

/**
 * Created by tobber on 5/3/15.
 */
/**
 * Created by tobber on 5/3/15.
 */
class SHist(xLabel : String = "", yLabel : String = "")
  extends SAbstractPlot[SHistSeries](xLabel, yLabel) {
  private val dataset = new HistogramDataset

  def += (series : SHistSeries) {
    dataset.addSeries(series.name, series.array, series.bins, series.min, series.max)
  }

  override protected def getDataset(): XYDataset = dataset
  override protected def createRenderer(imageScale : Double) = {
    val renderer =  new XYBarRenderer()
    renderer.setGradientPaintTransformer(null)
    renderer.setShadowVisible(false)
    renderer.setBarPainter(new StandardXYBarPainter)
    val w = 30.0 * imageScale
    val h = 50.0 * imageScale
    renderer.setLegendBar(new Rectangle2D.Double(-w/2.0, -h/2.0, w, h))
    renderer
  }
}


class SHistSeries(val name : String, val array : Array[Double], val bins : Int, val min : Double, val max : Double )
