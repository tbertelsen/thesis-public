package splot

import java.awt.geom.Rectangle2D
import java.awt._

import org.jfree.chart.ChartColor
import org.jfree.chart.plot.DefaultDrawingSupplier
import org.jfree.chart.plot.DefaultDrawingSupplier._
import org.jfree.util.ShapeUtilities

/**
 * Created by tobber on 5/3/15.
 */
class ScalableDrawingSupplier(shapeSize : Double, lineThickness : Double) extends DefaultDrawingSupplier(
  ScalableDrawingSupplier.defaultColors,
  DEFAULT_FILL_PAINT_SEQUENCE,
  DEFAULT_OUTLINE_PAINT_SEQUENCE,
  ScalableDrawingSupplier.getStrokeSequence(lineThickness),
  DEFAULT_OUTLINE_STROKE_SEQUENCE,
  ScalableDrawingSupplier.getShapeSequence(shapeSize)) {
}

object ScalableDrawingSupplier {

  def apply(scale : Double) = {
    new ScalableDrawingSupplier(7.0 * scale, 3.0 * scale)
  }

  def getShapeSequence(size : Double) : Array[Shape] = {
    val small = size / 2.0D;
    val tiny = small / 2.0D;
    val sizeF = size.toFloat / 2.0F
    val thickF = tiny.toFloat / 3.0F

    Array[Shape](
      new java.awt.geom.Ellipse2D.Double(-small, -small, size, size),
      ShapeUtilities.createDiagonalCross(sizeF, thickF),
      ShapeUtilities.createDownTriangle(sizeF),
      ShapeUtilities.createRegularCross(sizeF, thickF),
      ShapeUtilities.createDiamond(sizeF),
      ShapeUtilities.createUpTriangle(sizeF),
      new Rectangle2D.Double(-small, -small, size, size),
      new Rectangle2D.Double(-small, -tiny, size, small),
      new Rectangle2D.Double(-tiny, -small, small, size)
    )
  }

  def getStrokeSequence(lineThickness: Double) : Array[Stroke] = {
    Array[Stroke](new BasicStroke(lineThickness.toFloat, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL))
  }

  def defaultColors() = Array[Paint] (
    ChartColor.DARK_BLUE,
    ChartColor.DARK_RED,
    ChartColor.DARK_GREEN,
    ChartColor.DARK_MAGENTA,
    ChartColor.DARK_CYAN,
    ChartColor.VERY_DARK_BLUE,
    ChartColor.VERY_DARK_RED,
    ChartColor.VERY_DARK_GREEN,
    ChartColor.VERY_DARK_MAGENTA,
    ChartColor.VERY_DARK_CYAN,
    ChartColor.LIGHT_BLUE,
    ChartColor.LIGHT_RED,
    ChartColor.LIGHT_GREEN,
    ChartColor.LIGHT_MAGENTA,
    ChartColor.LIGHT_CYAN
  );
}
