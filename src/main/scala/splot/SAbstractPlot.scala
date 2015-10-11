package splot

import java.awt.geom.{AffineTransform, Rectangle2D}
import java.awt.{Font, Color, Paint}
import java.io.{FileOutputStream, File}
import javax.imageio.ImageIO

import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.{NumberAxis, LogarithmicAxis}
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.{AbstractXYItemRenderer, XYItemRenderer}
import org.jfree.data.xy.{XYSeries, XYSeriesCollection, XYDataset}
import org.jfree.ui.RectangleInsets

/**
 * Created by tobber on 5/3/15.
 */
abstract class SAbstractPlot[T](var xLabel : String = "", var yLabel : String = "") {

    private var xLog = false
    private var yLog = false
    private var minLog = 0.1
    var width = 1800
    var height = 1200

    var itemScale = 1.0
    var legendScale = 4.0
    var opacity = 0.8
    var fontScale = 1.0

    def += (series : T)

    protected def getDataset() : XYDataset
    protected def createRenderer(scale : Double) : AbstractXYItemRenderer

    def log(minLog : Double = 0.1) = {
      this.minLog = minLog
      yLog = true
      this
    }

    def logLog(minLog : Double = 0.1) = {
      this.minLog = minLog
      xLog = true
      yLog = true
      this
    }

    def resolution(width : Int, height : Int) = {
      this.width = width
      this.height = height
      this
    }

    private def axis(label: String, log : Boolean) = {
      val axis = if (log) {
        val axis = new LogarithmicAxis(label)
        axis.setStrictValuesFlag(false)
        axis
      } else {
        new NumberAxis(label)
      }
      axis.setLabelFont(normalFont())
      axis.setTickLabelFont(tickFont())
      axis
    }

    private def normalFont() = new Font("SansSerif", Font.PLAIN, (55 * fontScale * imageScale()).toInt)
    private def tickFont() = new Font("SansSerif", Font.PLAIN, (40 * fontScale * imageScale()).toInt)

    def imageScale() = height / 1200.0

    def save(path : String) : Unit = save(new File(path))
  
    def save(file : File) : Unit = {

      val extension = file.getName.split('.').last
      if ("png" != extension) {
        throw new IllegalArgumentException{"Unsupported file type: " + extension}
      }
      val data = getDataset()

      // Show legend unless all keys are empty
      val showLegend = (0 until data.getSeriesCount) map {data.getSeriesKey(_).toString} exists(_.length > 0)

      val xAxis = axis(xLabel, xLog)
      val yAxis = axis(yLabel, yLog)

      val renderer = createRenderer(imageScale())
      val plot = new XYPlot(data, xAxis, yAxis, renderer);
      plot.setDrawingSupplier(ScalableDrawingSupplier(itemScale * imageScale()))
      plot.setForegroundAlpha(opacity.toFloat)
      val chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, showLegend);
      // Scale legend
      if (chart.getLegend != null) {
        val legend = chart.getLegend
        legend.setItemFont(normalFont())
        val pad = (10 * imageScale()).toInt
        legend.setItemLabelPadding(new RectangleInsets(pad, pad, pad, 2 * pad))
        legend.setLegendItemGraphicPadding(new RectangleInsets(pad, 2 * pad, pad, 0))
      }
      // Make background transparent
      chart.setBackgroundPaint(null)
      // Ensure we do not cut large text:
      val chartPad = 25 * imageScale()
      chart.setPadding(new RectangleInsets(chartPad, chartPad, chartPad, chartPad))

      // Ensure shapes has the right order:
      // And scale the legend shape
      val legendTransform = AffineTransform.getScaleInstance(legendScale/itemScale, legendScale/itemScale)
      for (i <- 0 until data.getSeriesCount) {
        val shape = renderer.lookupSeriesShape(i)
        val legendShape = legendTransform.createTransformedShape(shape)
        renderer.setLegendShape(i, legendShape)
        val item = renderer.getLegendItem(0,i)
      }

      val bufferedImage = chart.createBufferedImage(width, height);
      val absFile = file.getAbsoluteFile
      absFile.getParentFile().mkdirs()
      ImageIO.write(bufferedImage, extension, new FileOutputStream(absFile));
    }
  }
