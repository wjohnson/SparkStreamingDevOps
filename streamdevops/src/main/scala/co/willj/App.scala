package co.willj

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD


import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.eventhubs.{ ConnectionStringBuilder, EventHubsConf, EventPosition }
import org.apache.spark.sql.functions.{ concat, explode, lit, split, window }

import org.apache.spark.sql._

import com.databricks.dbutils_v1.DBUtilsHolder.dbutils

object SampleStreamingApp {

  def customTokenize(wordCol: Column): Column = {
    return explode(split(wordCol.cast("string"), "\\s+"))
  }

  def main (arg: Array[String]): Unit = {

  val secretScope = arg(0)
  val ehubConnectionString = arg(1)
  val ehubName = arg(2)
  val ehubConsumerGroup = arg(3)
  val checkpointLocation = arg(4)
  val streamOutputLocation = arg(5)

  val spark = SparkSession.builder().appName("SampleStreamingApp").getOrCreate()
  import spark.implicits._

// To connect to an Event Hub, EntityPath is required as part of the connection string.
// Here, we assume that the connection string from the Azure portal does not have the EntityPath part.
val inputconnectionString = ConnectionStringBuilder(dbutils.secrets.get(scope=secretScope, key=ehubConnectionString))
  .setEventHubName(ehubName)  
  .build
val inputeventHubsConf = EventHubsConf(inputconnectionString)
  .setStartingPosition(EventPosition.fromEndOfStream)
  .setConsumerGroup(ehubConsumerGroup)

val eventhubs = spark.readStream
  .format("eventhubs")
  .options(inputeventHubsConf.toMap)
  .load()

// COMMAND ----------

// split lines by whitespaces and explode the array as rows of 'word'
val df = eventhubs
  .select($"enqueuedTime", customTokenize($"body").as("word"))
  .withWatermark("enqueuedTime", "2 minutes")
  .groupBy(
    window($"enqueuedTime", "4 minutes", "2 minutes"),
    $"word"
  )
  .count
  .withColumn("body", concat($"word",lit("-"), $"count".cast("string")))

// COMMAND ----------

//display(dbutils.fs.ls("/tmp/events/wordcount"))
//dbutils.fs.rm("/tmp/events/wordcount", recurse=true)

// COMMAND ----------

df.writeStream
  .trigger(Trigger.ProcessingTime("60 seconds"))
  .format("parquet")
  .outputMode("append")
  .option("checkpointLocation", checkpointLocation)
  .option("path", streamOutputLocation)
  .start()

// COMMAND ----------



  }
}