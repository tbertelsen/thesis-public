package apps

import java.text.SimpleDateFormat
import java.util.Date

class Timer {
  
  private var timeMs = System.currentTimeMillis();
  
  def reset() = {
    timeMs =  System.currentTimeMillis();
  }
  
  def getSeconds() = {
    (System.currentTimeMillis() - timeMs) / 1000.0
  }

}

object Timer {
  val form = "%-30s"
  val defaultTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  def log(s : String) = {
    printf("%s : %s%n",timeStamp(),s)
  }

  def timeStamp() = {
    defaultTimestamp.format(new Date())
  }

  def time[T](name: String)(f : () => T) : T = {
    printf(form, name)
    val t = new Timer
    val ret = f()
    printf(" Done in %.2f s.%n", t.getSeconds)
    ret
  }

  def getSeconds[T](f : () => T) : (T,Double) = {
    val t = new Timer
    val ret = f()
    val sec = t.getSeconds()
    (ret, sec)
  }
}


