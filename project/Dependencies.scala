import sbt._

object Version {
  val zio        = "1.0.3"
  val circe      = "0.12.3"
  val sttp       = "3.0.0-RC11"
  val pureConfig = "0.14.0"
  val jsoup      = "1.13.1"
  val zioLogging = "0.4.0"
}

object Dependencies {

  val mainDeps =
    Seq(
      "dev.zio"                       %% "zio"                    % Version.zio,
      "dev.zio"                       %% "zio-logging"            % Version.zioLogging,
      "io.circe"                      %% "circe-generic"          % Version.circe,
      "io.circe"                      %% "circe-parser"           % Version.circe,
      "com.softwaremill.sttp.client3" %% "core"                   % Version.sttp,
      "com.github.pureconfig"         %% "pureconfig"             % Version.pureConfig,
      "org.jsoup"                      % "jsoup"                  % Version.jsoup,
      "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % Version.sttp
    )

  val testDeps =
    Seq(
      "dev.zio" %% "zio-test"     % Version.zio,
      "dev.zio" %% "zio-test-sbt" % Version.zio
    ).map(_ % Test)
}
