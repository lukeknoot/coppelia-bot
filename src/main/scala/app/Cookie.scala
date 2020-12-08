package app

import zio._
import AppState.AppState
import zio.clock._
import java.util.concurrent.TimeUnit
import zio.logging.Logging
import zio.logging._

object Cookie {

  /** We consider a valid cookie to be one that has no expiry or
    * will expire in at least 5 minutes from now.
    */
  def hasValidCookie: ZIO[AppState with Clock with Logging, Nothing, Boolean] = for {
    cookies    <- ZIO.accessM[AppState](_.get.get)
    now        <- currentTime(TimeUnit.MILLISECONDS)
    _          <- log.info(s"Checking for cookies $cookies")
    fiveMinutes = 1000 * 60 * 5
  } yield {
    cookies.nonEmpty && cookies.forall(
      _.expires.map(c => c.toEpochMilli() > now + fiveMinutes).getOrElse(true)
    )
  }

}
