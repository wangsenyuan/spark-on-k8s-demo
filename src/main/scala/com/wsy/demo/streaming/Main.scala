package com.wsy.demo.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Main {

  /** Our main function where the action happens */
  def main(args: Array[String]) {
    runStreaming()
  }

  //  val filePath = "hdfs://192.168.95.152:9000/user/spark/checkpoints/"

  def createStreamingJob(checkpointDir: String): () => StreamingContext = () => {
    val conf = new SparkConf()
      .setAppName("Spark Streaming Job")
      .setIfMissing("spark.master", "local[*]")

    val ssc = new StreamingContext(conf, Seconds(1))

    ssc.checkpoint(checkpointDir)

    val lines = ssc.socketTextStream("192.168.102.103", 9999)

    val words = lines.flatMap(_.split(" "))
    // Count each word in each batch
    val pairs = words.map(word => (word, 1))

    val wordCounts = pairs.reduceByKey(_ + _)

    // Print the first ten elements of each RDD generated in this DStream to the console
    wordCounts.print()

    val runningCounts = pairs.updateStateByKey(updateFunction _)

    runningCounts.print()

    ssc
  }

  def runStreaming(): Unit = {
    val checkpointDirectory = sys.env.get("CHECKPOINT_DIR").get

    println(checkpointDirectory)

    val ssc = StreamingContext.getOrCreate(checkpointDirectory, createStreamingJob(checkpointDirectory))
    //    ssc.checkpoint(checkpointDirectory.get)
    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate
  }

  private def updateFunction(newValues: Seq[Int], runningCount: Option[Int]): Option[Int] = {
    println("updateFunction called")
    val newSum = newValues.sum
    runningCount match {
      case Some(v) => Some(v + newSum)
      case None => Some(newSum)
    }
  }
}
