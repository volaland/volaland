val sparkVersion = "2.3.1"
val catsCoreVersion = "1.4.0"
val catsEffectVersion = "1.0.0"
val catsMtlVersion = "0.3.0"
val shapeless = "2.3.2"

val scalatest = "3.0.3"
val scalacheck = "1.13.5"

lazy val volalandSettings = Seq(
  organization := "land.vola",
  scalaVersion := "2.11.12",
  scalacOptions ++= commonScalacOptions,
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % shapeless,
    "org.scalatest" %% "scalatest" % scalatest % "test",
    "org.scalacheck" %% "scalacheck" % scalacheck % "test"),
  javaOptions in Test ++= Seq("-Xmx1G", "-ea"),
  fork in Test := true,
  parallelExecution in Test := false
)

lazy val commonScalacOptions = Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-Xfuture"
)


lazy val root = Project("volaland", file("." + "volaland")).in(file("."))
  .aggregate(core, cats, dataset, ml, docs)
  .settings(volalandSettings: _*)

lazy val core = project
  .settings(name := "volaland-core")
  .settings(volalandSettings: _*)


lazy val cats = project
  .settings(name := "volaland-cats")
  .settings(volalandSettings: _*)
  .settings(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    scalacOptions += "-Ypartial-unification"
  )
  .settings(libraryDependencies ++= Seq(
    "org.typelevel"    %% "cats-core"      % catsCoreVersion,
    "org.typelevel"    %% "cats-effect"    % catsEffectVersion,
    "org.typelevel"    %% "cats-mtl-core"  % catsMtlVersion,
    "org.typelevel"    %% "alleycats-core" % catsCoreVersion,
    "org.apache.spark" %% "spark-core"     % sparkVersion % "provided",
    "org.apache.spark" %% "spark-streaming"      % sparkVersion % "provided"))
  .dependsOn(dataset % "test->test;compile->compile")

lazy val dataset = project
  .settings(name := "volaland-dataset")
  .settings(volalandSettings: _*)
  .settings(GTBDatasetREPL: _*)
  .settings(libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-streaming"  % sparkVersion % "provided"
  ))
  .dependsOn(core % "test->test;compile->compile")

lazy val blockchain = project
  .settings(name := "volaland-blockchain")
  .settings(volalandSettings: _*)
  .settings(GTBDatasetREPL: _*)
  .settings(libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-streaming"  % sparkVersion % "provided"
  ))
  .dependsOn(core % "test->test;compile->compile")

lazy val ml = project
  .settings(name := "volaland-ml")
  .settings(volalandSettings: _*)
  .settings(GTBDatasetREPL: _*)
  .settings(libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-streaming"  % sparkVersion % "provided",
    "org.apache.spark" %% "spark-mllib"  % sparkVersion % "provided"
  ))
  .dependsOn(
    core % "test->test;compile->compile",
    dataset % "test->test;compile->compile"
  )

lazy val docs = project
  .settings(volalandSettings: _*)
  .settings(tutSettings: _*)
  .settings(crossTarget := file(".") / "docs" / "target")
  .settings(libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-streaming"  % sparkVersion,
    "org.apache.spark" %% "spark-mllib"  % sparkVersion
  ))
  .settings(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    scalacOptions += "-Ypartial-unification"
  )
  .dependsOn(dataset, cats, ml)

lazy val GTBDatasetREPL = Seq(
  initialize ~= { _ => // Color REPL
    val ansi = System.getProperty("sbt.log.noformat", "false") != "true"
    if (ansi) System.setProperty("scala.color", "true")
  },
  initialCommands in console :=
    """
      |import org.apache.spark.{SparkConf, SparkContext}
      |import org.apache.spark.sql.SparkSession
      |import volaland.functions.aggregate._
      |import volaland.syntax._
      |
      |val conf = new SparkConf().setMaster("local[*]").setAppName("volaland repl").set("spark.ui.enabled", "false")
      |implicit val spark = SparkSession.builder().config(conf).appName("REPL").getOrCreate()
      |
      |import spark.implicits._
      |
      |spark.sparkContext.setLogLevel("WARN")
      |
      |import volaland.TypedDataset
    """.stripMargin,
  cleanupCommands in console :=
    """
      |spark.stop()
    """.stripMargin
)

copyReadme := copyReadmeImpl.value
lazy val copyReadme = taskKey[Unit]("copy for website generation")
lazy val copyReadmeImpl = Def.task {
  val from = baseDirectory.value / "README.md"
  val to   = baseDirectory.value / "docs" / "src" / "main" / "scala" / "README.md"
  sbt.IO.copy(List((from, to)), overwrite = true, preserveLastModified = true)
}
