Global / onChangedBuildSource := ReloadOnSourceChanges

val mainCompilerOptions = Seq(
  "-encoding",
  "UTF-8",                 // source files are in UTF-8
  "-deprecation",          // warn about use of deprecated APIs
  "-unchecked",            // warn about unchecked type parameters
  "-feature",              // warn about misused language features
  "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
  "-Xlint"                 // enable handy linter warnings
)

lazy val app = (project in file("."))
  .settings(
    name := "coppelia-bot",
    organization := "net.lukeknight",
    scalaVersion := "2.13.4",
    version := "0.0.1",
    scalacOptions ++= mainCompilerOptions,
    javaOptions in reStart += "-Dconfig.resource=application.dev.conf",
    libraryDependencies ++= Dependencies.mainDeps,
    libraryDependencies ++= Dependencies.testDeps,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    dockerBaseImage := "openjdk:13.0.2",
    dockerUpdateLatest := true,
    // https://github.com/sbt/sbt-native-packager/issues/853
    // Can't specify only for docker build
    javaOptions in Universal ++= Seq(
      "-Dconfig.file=/application.conf"
    )
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
