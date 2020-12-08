package app

import pureconfig._
import zio._
import pureconfig.generic.auto._

case class Config(mboUsername: String, mboPassword: String)

object Config {

  val loadConfig = ZIO.service[Config]

  val live =
    ZIO.fromEither(ConfigSource.default.load[Config]).mapError(_.toString()).toLayer

}
