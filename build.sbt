val sparkVersion = "3.1.1"

ThisBuild / scalaVersion := "2.12.16"

val sparkLibs = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion

)

// JAR build settings
lazy val commonSettings = Seq(
  organization := "com.wsy",
  version := "0.0.1-SNAPSHOT",
  Compile / scalaSource := baseDirectory.value / "src" / "main" / "scala",
  Test / scalaSource := baseDirectory.value / "src" / "test" / "scala",
  Test / resourceDirectory := baseDirectory.value / "src" / "test" / "resources",
  javacOptions ++= Seq(),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps"
  ),
  libraryDependencies ++= sparkLibs
)


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    name := "spark-sql-demo",
    commonSettings,
    assembly / mainClass := Some("com.wsy.demo.pi.Main"),
    assembly / assemblyJarName := "spark-sql-demo.jar"
  )
