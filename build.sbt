name := "zio"
version := "0.1"
scalaVersion := "2.13.8"
//scalaVersion := "3.1.3"


lazy val zioVersion = "2.0.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-test" % zioVersion,
  "dev.zio" %% "zio-test-sbt" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-test-junit" % zioVersion,
  "dev.zio" %% "zio-json" % "0.3.0-RC8",

  "dev.zio" %% "zio-kafka" % "2.0.0",

  "io.d11" %% "zhttp" % "2.0.0-RC11"

  // For zio http


  // "dev.zio" %% "zio.kafka" % "2.0.0"



)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")