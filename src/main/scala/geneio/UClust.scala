package geneio

import javax.management.Query

/**
 * Created by tobber on 19/3/15.
 */
object UClust {

}

class UClustLine(val line : String) {
  val split = line.split("\t")
  val tpe = split(0)
  val cluster =   split(1)
  val size =      split(2)
  val identity100 = split(3)
  val strand =    split(4)
  val qlo =       split(5)
  val tlo =       split(6)
  val alignment = split(7)
  val query =     split(8)
  val target =    split(9)
}
