package volaland
package cats

import _root_.cats.Foldable
import _root_.cats.implicits._

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext => SC}

import org.scalatest.compatible.Assertion
import org.scalactic.anyvals.PosInt
import org.scalatest.Matchers
import org.scalacheck.Arbitrary
import org.scalatest._
import Arbitrary._
import prop._

import scala.collection.immutable.SortedMap
import scala.reflect.ClassTag

trait SparkTests {
  val appID: String = new java.util.Date().toString + math.floor(math.random * 1000).toLong.toString

  val conf: SparkConf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("gtb-test")
    .set("spark.ui.enabled", "false")
    .set("spark.app.id", appID)

  implicit def session: SparkSession = SparkSession.builder().config(conf).getOrCreate()
  implicit def sc: SparkContext = session.sparkContext

  implicit class seqToRdd[A: ClassTag](seq: Seq[A])(implicit sc: SC) {
    def toRdd: RDD[A] = sc.makeRDD(seq)
  }

}


class Test extends PropSpec with Matchers with PropertyChecks with SparkTests {
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(minSize = PosInt(10))

  property("spark is working") {
    sc.parallelize(Array(1, 2, 3)).collect shouldBe Array(1,2,3)
  }

}
