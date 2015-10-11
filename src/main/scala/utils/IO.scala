package utils

import java.io._
import java.net.URI


/**
 * Created by tobber on 13/3/15.
 */
object IO {
  def printToFile(f: java.io.File, append: Boolean = false)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(new FileWriter(f,append))
    try { op(p) } finally { p.close() }
  }

  def stringsToFile(f: java.io.File, strings: Iterable[Any], append: Boolean = false): Unit = {
    f.getParentFile.mkdirs()
    printToFile(f, append){w =>
      strings.foreach(w.println(_))
    }
  }

  def serialize[T](file: File, value:T) = {
    val oos = new ObjectOutputStream(new FileOutputStream(file))
    try {
      oos.writeObject(value)
    } finally { oos.close }
  }

  def deserialize[T](file: File) : T = {
    val ois = new ObjectInputStream(new FileInputStream(file))
    try {
      ois.readObject.asInstanceOf[T]
    } finally { ois.close }
  }

  def fileOrURI(path: String) = {
    if (path.contains(":")) {
      new URI(path)
    } else {
      new File(path).toURI
    }
  }

  def fileNameFromURI(uri: URI) = {
    uri.toString.split("[\\\\/]").last
  }

  def safeFileName(candidate: String) = {
    candidate.replaceAll("[\\s/:;\\\\]", "_")
  }

}
