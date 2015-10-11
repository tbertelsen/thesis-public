import breeze.numerics.log
import org.jfree.chart.renderer.xy.{XYBarRenderer, XYLineAndShapeRenderer}
import org.jfree.data.statistics.HistogramDataset
import org.jfree.data.xy.XYSeries

import scala.collection.immutable.NumericRange
import scala.collection.immutable.Range.Partial

/**
 * Created by tobber on 5/3/15.
 */
package object splot {

  def function(f : Double => Double, xs : Seq[Double] = 0.0 to 1.0 by 0.001, name : String = "") : XYSeries = {
    val data = xs map (x => (x, f(x)))
    points(data, name)
  }

  def points(xs : Seq[Double], ys : Seq[Double], name : String) : XYSeries = {
    if (xs.length != ys.length) {
      throw new IllegalArgumentException("x and y must have same length. %d != %d".format(xs,ys))
    }
    if (!xs.isTraversableAgain || !ys.isTraversableAgain) {
      throw new IllegalArgumentException("x or y is not traversable again")
    }
    points(xs zip ys, name)
  }

  def points(data : Seq[(Double, Double)], name : String) : XYSeries = {
    val series = new XYSeries(name)
    data.foreach(t => series.add(t._1, t._2))
    series
  }

  def hist(data : Seq[Double], name : String = "", bins : Int = 100) = {
    new SHistSeries(name, data.toArray, bins, data.min, data.max)
  }

  def histRange(data : Seq[Double], name : String = "", bins : NumericRange[Double] = 0.0 to 1.0 by 0.01) = {
    new SHistSeries(name, data.toArray, bins.length - 1, bins.min, bins.max)
  }

  def scatterPlot(xLabel : String = "", yLabel : String = "") = {
    new SPlot(false, true, xLabel, yLabel)
  }

  def linePlot(xLabel : String = "", yLabel : String = "") = {
    new SPlot(true, false, xLabel, yLabel)
  }

  def histPlot(xLabel : String = "", yLabel : String = "Frequency") = {
    new SHist(xLabel, yLabel)
  }

}
