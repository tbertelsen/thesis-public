// Info

name := "Master Thesis"

version := "0.1"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature", "-target:jvm-1.7", "-deprecation")

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

assemblyJarName in assembly := "thesis.jar"

// EclipseKeys.withSource := true

////////////////////////////////
// DEPENDENCIES:

// CORE DEPENDENCIES:

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"

libraryDependencies += "org.rogach" %% "scallop" % "0.9.5"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "1.3.0" % "provided",
    "org.apache.spark" %% "spark-graphx" % "1.3.0" % "provided",
    "org.apache.spark" %% "spark-mllib" % "1.3.0" % "provided"
)

// GUI / LOCAL DEPENDENCIES
// These should probably be removed when the real algorithm is developed
// (Split code up into projects, like core, cluster, gui)

libraryDependencies ++= Seq(
    "org.scalanlp" %% "breeze" % "0.11.1",
    "org.scalanlp" %% "breeze-natives" % "0.11.1",
    "org.jfree" % "jfreechart" % "1.0.14"
)

resolvers ++= Seq(
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)
