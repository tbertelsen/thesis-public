package apps

import java.io.File
import java.net.URI

import breeze.linalg.{max, *}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

object SimpleSpark {
  val mediumCache: StorageLevel = StorageLevel.MEMORY_ONLY_SER
  val largeCache: StorageLevel = StorageLevel.MEMORY_ONLY_SER

  val fa30 = "file:/eva/users/dkn957/data/30.fa"
  val fa210 = "file:/eva/users/dkn957/data/210.fa"
  val fa1495 = "file:/eva/users/dkn957/data/1495.fa"
  val fa2000 = "file:/eva/users/dkn957/data/HMP253.2000.fa"

  def connect(logLevel : Level = Level.WARN) : SparkContext = {
    startSpark("spark://porus06:7077", "Porus Spark", logLevel)
  }

  def startSparkLocal(threads: Int, logLevel : Level = Level.WARN): SparkContext = {
    startSpark("local[" + threads + "]", "Local Spark", logLevel)
  }

  def startSpark(master : String, name : String, logLevel : Level = Level.WARN): SparkContext = {
    setLogLevel(logLevel)
    val sconf = new SparkConf()
      .setMaster(master)
      .setAppName(name)
    new SparkContext(sconf)
  }

  def setLogLevel(level: String): Unit = {
    setLogLevel(Level.toLevel(level, Level.INFO))
  }

  def setLogLevel(level: Level): Unit = {
    Logger.getLogger("org").setLevel(level)
    Logger.getLogger("akka").setLevel(level)
  }

  def delimitedFile(sc: SparkContext, uri: URI, delim: String) : RDD[(Long, String)] = {
    val conf = new Configuration()
    conf.set("textinputformat.record.delimiter", delim)
    conf.set("mapred.max.split.size", "64000000")
    val rdd = sc.newAPIHadoopFile(uri.toString, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], conf)
      .map { case (lw, text) => (lw.get(), text.toString)}

    rdd.repartition(defaultPartitions(sc))
  }

  def defaultPartitions(sc : SparkContext) = {
    sc.defaultParallelism
  }

  /**
   * Finds the values for each key in a pair of keys.
   * @param pairs The pairs of keys to lookup.
   * @param data The index from key to value.
   * @tparam V The value type
   * @return An RDD of `((id1, id2),(val2,val2))` where `(id1,id2)` is in `pairs` and
   *         `(id1,val1)` and `(id2,val2)` are in `data`
   */
  def pairLookup[V](pairs : RDD[(Long, Long)], data : RDD[(Long, V)]) : RDD[((Long,Long),(V,V))] = {
    val firstJoin = pairs.map(pair => (pair._1, pair)).join(data)
    val secondJoin = firstJoin.map{case (_, (pair,val1)) => (pair._2, (pair,val1))}.join(data)
    secondJoin.map{case(_, ((pair, val1), val2)) => (pair, (val1, val2))}
  }
}
