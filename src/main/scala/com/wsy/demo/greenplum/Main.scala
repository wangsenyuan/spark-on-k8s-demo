package com.wsy.demo.greenplum

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.avg

object Main {

  def main(args: Array[String]): Unit = {
    println("--- env map ---- ")
    println(scala.sys.env.toMap)
    println("--- env map ---- ")

    val spark = createSparkContext()

    import spark.implicits._

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

    //    println(jdbcDF.count())

    val df1 = jdbcDF.where(jdbcDF("ent_code") === "EC1609211LPZWMBK").toDF()
    val df2 = df1.groupBy("form_cover_department_code").agg(avg($"form_amount").alias("form_amount"))

    df2.printSchema()

    df2.show(100)
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
