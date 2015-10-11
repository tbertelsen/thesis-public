package apps

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.Subcommand
import org.omg.CORBA.Object

class ProxyArgs(stringArgs: Seq[String]) extends ScallopConf(stringArgs) {
  printedName = "thesis"

  val align = new ProxyArgs.Align
  val calcProxy = new ProxyArgs.CalcProxy
  val plot = new ProxyArgs.Plot
  val stats = new ProxyArgs.stats
  val kstats = new ProxyArgs.kstats

  ConfHelp.defaultTweak(this)
}

object ProxyArgs {
  class Align extends ProxyCommand("align") {
    descr("Calculate alignment proxies")
  }
  class CalcProxy extends ProxyCommand("calcProxy") {
    descr("Calculate alignment proxies")
  }
  class Plot extends ProxyCommand("plot") {
    descr("Plot alignment proxies")
    val minAlign = opt[Int](default = Some(0))
  }
  class stats extends ProxyCommand("stats") {
    descr("Do statistics on proxies and genes")
  }

  class kstats extends Subcommand("kstats") {
    descr("Do statistics on proxies and genes across a range of k's")
    val dataset = opt[String](required = true)
    val minK = opt[Int](default = Some(3), validate = x => 0 < x && x <= 15)
    val maxK = opt[Int](default = Some(15), validate = x => 0 < x && x <= 15)
    ConfHelp.defaultTweak(this)
  }

  abstract class ProxyCommand(name: String) extends Subcommand(name) {
    val dataset = opt[String](required = true)
    val k = opt[Int](default = Some(8), validate = x => 0 < x && x <= 15)
    ConfHelp.defaultTweak(this)
  }
}
