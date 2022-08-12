package com.wsy.demo.hdfs

import org.apache.spark.{SparkConf, SparkContext}

object Main {
  /** Our main function where the action happens */
  def main(args: Array[String]) {
    runText()
  }


  case class Person(ID: Int, name: String, age: Int, numFriends: Int)

  def mapper(line: String): Person = {
    val fields = line.split(',')

    val person: Person = Person(fields(0).toInt, fields(1), fields(2).toInt, fields(3).toInt)
    person
  }


  val filePath = "hdfs://192.168.95.152:9000/user/spark/fakefriends.csv"

  def runText(): Unit = {
    val conf = new SparkConf()
      .setAppName("Spark Job")
      .setIfMissing("spark.master", "local[*]")

    val sc = new SparkContext(conf)

    val file = sc.textFile(filePath)

    val people = file.map(mapper)

    println("show people name")
    people.map(p => p.name).collect().foreach(println)

    println("show person age < 21")
    people.filter(p => p.age < 21).collect().foreach(println)

    sc.stop()
  }
}
