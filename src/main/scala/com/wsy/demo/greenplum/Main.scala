package com.wsy.demo.greenplum

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object Main {

  def main(args: Array[String]): Unit = {
    println("--- env map ---- ")
    println(scala.sys.env.toMap)
    println("--- env map ---- ")

    val spark = createSparkContext()

    val jdbcDF = spark.read
      .format("jdbc")
      .option("driver", "org.postgresql.Driver")
      .option("url", "jdbc:postgresql://118.31.105.14:3559/prod_perf_test")
      .option("dbtable", "(select gp_segment_id, * from custom_reimburse_data_detail) as custom_reimburse_data_detail")
      .option("user", "prod_user1")
      .option("password", "VeeEdD9SVm2SQj")
      .option("partitionColumn", "gp_segment_id")
      .option("numPartitions", 4)
      .option("lowerBound", 0)
      .option("upperBound", 19)
      .load()


    println(jdbcDF.count())
  }


  private def createSparkContext(): SparkSession = {
    val conf = new SparkConf()
      .setAppName("Spark Job")
      .setIfMissing("spark.master", "local[*]")

    SparkSession
      .builder.config(conf)
      .getOrCreate()
  }
}
