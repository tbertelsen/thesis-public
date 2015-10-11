package apps

import org.rogach.scallop.ScallopConf

/**
 * Created by tobber on 12/3/15.
 */

object ConfHelp {
  def defaultTweak(conf: ScallopConf) = {
    conf.banner("Usage:")
    conf.errorMessageHandler = { message =>
      if (System.console() == null) {
        // no colors on output
        println("[%s] Error: %s" format (conf.printedName, message))
      } else {
        println("[\033[31m%s\033[0m] Error: %s" format (conf.printedName, message))
      }
      conf.builder.printHelp
      throw new RuntimeException()
    }
  }
}
