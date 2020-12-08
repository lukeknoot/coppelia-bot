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
    name := "dance-auto-booking",
    organization := "net.lukeknight",
    scalaVersion := "2.13.2",
    version := "0.0.1",
    scalacOptions ++= mainCompilerOptions,
    javaOptions in reStart += "-Dconfig.resource=application.dev.conf",
    libraryDependencies ++= Dependencies.mainDeps,
    libraryDependencies ++= Dependencies.testDeps,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    dockerExposedPorts ++= Seq(8080)
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
