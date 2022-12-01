package com.wsy.demo.greenplum

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object Main {

  def main(args: Array[String]): Unit = {
    println("--- env map ---- ")
    println(scala.sys.env.toMap)
    println("--- env map ---- ")

    val spark = createSparkContext()
    import spark.implicits._

    //    val gpdf = spark.read.greenplum(url, tblname, jprops)
    def randomInt1to100 = scala.util.Random.nextInt(100) + 1

    val df = spark.sparkContext.parallelize(
      Seq.fill(100) {
        (randomInt1to100, randomInt1to100, randomInt1to100)
      }
    ).toDF("col1", "col2", "col3")

    df.show(false)

    val gscWriteOptionMap = Map(
      "url" -> "jdbc:postgresql://192.168.95.153:5432/wsy",
      "user" -> "wsy_user1",
      "password" -> "VeeEdD9SVm2SQj",
      "dbschema" -> "public",
      "dbtable" -> "test_write",
      "server.port" -> "12900",
      "server.useHostname" -> "false",
      "server.hostEnv" -> "env.NODE_IP"
    )

    import org.apache.spark.sql.SaveMode

    df.write.format("greenplum").options(gscWriteOptionMap).mode(SaveMode.Overwrite).save()

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
