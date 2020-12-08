package app

import zio.ZEnv
import zio.logging._
import sttp.client3.httpclient.zio.HttpClientZioBackend

object AppEnv {

  val live =
    ZEnv.live ++
      SDCService.live ++
      Config.live ++
      MBOService.live ++
      AppState.live ++
      Logging.console() ++
      HttpClientZioBackend.layer()

}
