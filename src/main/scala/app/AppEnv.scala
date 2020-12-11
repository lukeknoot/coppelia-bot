package app

import zio.logging._
import sttp.client3.httpclient.zio.HttpClientZioBackend

object AppEnv {

  val live =
    SDCService.live ++
      Config.live ++
      MBOService.live ++
      AppState.live ++
      Logging.console() ++
      HttpClientZioBackend.layer() ++
      TelegramService.live

}
