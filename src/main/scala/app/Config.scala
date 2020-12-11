package app

import pureconfig._
import zio._
import pureconfig.generic.auto._

case class Config(
    mbo: MBOConfig,
    telegram: TelegramConfig
)

case class MBOConfig(username: String, password: String)

case class TelegramConfig(token: String, chatID: String)

object Config {

  val loadConfig = ZIO.service[Config]

  val live =
    ZIO
      .fromEither(ConfigSource.default.load[Config])
      .toLayer

}
