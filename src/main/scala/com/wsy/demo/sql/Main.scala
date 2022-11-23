package com.wsy.demo.sql

import com.wsy.demo.sql.FirstPredicateAggregateWindowFunction.first_predicate
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession, functions}

object Main {

  case class Person(ID: Int, name: String, age: Int, numFriends: Int)

  def mapper(line: String): Person = {
    val fields = line.split(',')

    val person: Person = Person(fields(0).toInt, fields(1), fields(2).toInt, fields(3).toInt)
    person
  }

  /** Our main function where the action happens */
  def main(args: Array[String]) {
    //    runSql()
    //    runWindowSample()
    runWindowSample1()
  }

  def runWindowSample1(): Unit = {
    val spark = createSparkContext()
    val sensorsDF = createSensorDf(spark)

    sensorsDF.show()

    val window = Window.partitionBy("place").orderBy(desc("time"))

    val res = sensorsDF.withColumn(
      "newest",
      first_predicate(
        col("error") === true,
        lit(true),
        lit(false)).over(window))
      .filter(col("newest") === true)
      .drop("newest")

    res.show()

    spark.stop()
  }

  def runWindowSample(): Unit = {
    val spark = createSparkContext()
    val sensorsDF = createSensorDf(spark)

    sensorsDF.show()

    val window = Window.partitionBy("place").orderBy(desc("time"))

    val res = sensorsDF
      .withColumn("rank", row_number.over(window)) // Step1
      .withColumn("rank", when(col("error") === true, col("rank")).otherwise(null)) // Step2
      .withColumn("min_rank", functions.min("rank").over(window)) // Step3
      .filter(col("rank").isNull and col("min_rank").isNull) // Step4
      .drop("rank", "min_rank", "error")

    res.show()

    spark.stop()
  }

  private def createSensorDf(spark: SparkSession): DataFrame = {
    import spark.implicits._
    val df = Seq(
      (12, "house", "2017-07-14 00:21:54", "10.0", false),
      (11, "house", "2017-07-14 00:18:10", "11.0", false),
      (10, "house", "2017-07-14 00:15:45", "11.0", false),
      (9, "house", "2017-07-14 00:13:14", "9999.0", true),
      (8, "house", "2017-07-14 00:10:18", "9.0", false),
      (7, "house", "2017-07-14 00:06:43", "9.0", false),
      (6, "house", "2017-07-14 00:02:16", "9999.0", true),
      (5, "house", "2017-07-14 00:00:30", "13.0", false),
      (4, "office", "2017-07-14 00:20:11", "22.0", false),
      (2, "office", "2017-07-14 00:17:56", "23.0", false),
      (2, "basement", "2017-07-14 00:23:47", "9999.0", true),
      (1, "basement", "2017-07-14 00:10:32", "9999.0", true)
    )
      .toDF("measure_id", "place", "time", "temperature", "error")
      .withColumn("time", to_timestamp(col("time")))

    df
  }

  private def createSparkContext(): SparkSession = {
    val conf = new SparkConf()
      .setAppName("Spark Job")
      .setIfMissing("spark.master", "local[*]")

    SparkSession
      .builder.config(conf)
      .getOrCreate()
  }

  def runSql(): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)
    val spark = createSparkContext()
    import spark.implicits._
    // Convert our csv file to a DataSet, using our Person case
    // class to infer the schema.
    val lines = spark.sparkContext.textFile("/app/data/fakefriends.csv")
    val people = lines.map(mapper).toDS().cache()

    println("Here is our inferred schema:")
    people.printSchema()

    println("Let's select the name column:")
    people.select("name").show()

    println("Filter out anyone over 21:")
    people.filter(people("age") < 21).show()

    println("Group by age:")
    people.groupBy("age").count().show()

    println("Make everyone 10 years older:")
    people.select(people("name"), people("age") + 10).show()

    spark.stop()
  }
}
