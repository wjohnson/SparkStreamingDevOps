// Databricks notebook source
// MAGIC %md
// MAGIC 
// MAGIC This is a WordCount example with the following:
// MAGIC * Event Hubs as a Structured Streaming Source
// MAGIC * Stateful operation (groupBy) to calculate running counts
// MAGIC 
// MAGIC #### Requirements
// MAGIC 
// MAGIC * Databricks version 3.5 or 4.0 and beyond. 

// COMMAND ----------

println("Hello!")

// COMMAND ----------

import spark.implicits._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.eventhubs.{ ConnectionStringBuilder, EventHubsConf, EventPosition }
import org.apache.spark.sql.functions.{ concat, explode, lit, split, window }

// COMMAND ----------

// To connect to an Event Hub, EntityPath is required as part of the connection string.
// Here, we assume that the connection string from the Azure portal does not have the EntityPath part.
val inputconnectionString = ConnectionStringBuilder(dbutils.secrets.get(scope="myproject", key="ehublistenconnstr" ))
  .setEventHubName("textehub")  
  .build
val inputeventHubsConf = EventHubsConf(inputconnectionString)
  .setStartingPosition(EventPosition.fromEndOfStream)
  .setConsumerGroup("sparkconsumer")

val eventhubs = spark.readStream
  .format("eventhubs")
  .options(inputeventHubsConf.toMap)
  .load()

// COMMAND ----------

// split lines by whitespaces and explode the array as rows of 'word'
val df = eventhubs
  .select($"enqueuedTime", explode(split($"body".cast("string"), "\\s+")).as("word"))
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
  .option("path", "/mnt/eventOutput/output/01/")
  .option("checkpointLocation", "/mnt/eventOutput/checkpoints/01/")
  .start()

// COMMAND ----------


