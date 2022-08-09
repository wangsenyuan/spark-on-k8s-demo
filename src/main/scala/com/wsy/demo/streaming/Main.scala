package com.wsy.demo.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Main {

  /** Our main function where the action happens */
  def main(args: Array[String]) {
    runStreaming()
  }

  def runStreaming(): Unit = {
    val conf = new SparkConf()
      .setAppName("Spark Job")
      .setIfMissing("spark.master", "local[*]")

    val ssc = new StreamingContext(conf, Seconds(1))
    val lines = ssc.socketTextStream("192.168.97.68", 9999)
    val words = lines.flatMap(_.split(" "))
    // Count each word in each batch
    val pairs = words.map(word => (word, 1))
    val wordCounts = pairs.reduceByKey(_ + _)

    // Print the first ten elements of each RDD generated in this DStream to the console
    wordCounts.print()

    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate
  }
}
