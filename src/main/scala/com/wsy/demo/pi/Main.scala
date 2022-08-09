package com.wsy.demo.pi

import org.apache.spark.{SparkConf, SparkContext}

object Main {
  /** Our main function where the action happens */
  def main(args: Array[String]) {
    runPi()
  }

  def runPi(): Unit = {
    val conf = new SparkConf()
      .setAppName("Spark Job")
      .setIfMissing("spark.master", "local[*]")

    val sc = new SparkContext(conf)
    val NUM_SAMPLES = 100000000

    val count = sc.parallelize(1 to NUM_SAMPLES).filter { _ =>
      val x = math.random
      val y = math.random
      x * x + y * y < 1
    }.count()
    println(s"Pi is roughly ${4.0 * count / NUM_SAMPLES}")

    sc.stop()
  }
}
