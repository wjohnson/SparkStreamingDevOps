package tests

import org.scalatest.FunSuite
import com.holdenkarau.spark.testing.SharedSparkContext 
import com.holdenkarau.spark.testing.DataFrameSuiteBase


import co.willj.SampleStreamingApp


class test extends FunSuite with DataFrameSuiteBase {
  test("simple test") {
    val sqlCtx = sqlContext
    import sqlCtx.implicits._

    val rawDF = sc.parallelize(
        List("Hello World")
    ).toDF("term")
    val expectedDF = sc.parallelize(
        List("Hello", "World")
    ).toDF("tokens")

    val resultDF = rawDF.select(SampleStreamingApp.customTokenize($"term").as("tokens"))
    assertDataFrameEquals(resultDF, expectedDF)
  }
}
