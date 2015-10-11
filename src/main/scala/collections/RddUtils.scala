package collections

import org.apache.spark.rdd.RDD

/**
 * Created by tobber on 13/2/15.
 */
object RddUtils {
  def isEmpty[T](rdd: RDD[T]): Boolean = {
    rdd.count() == 0;
  }
}
