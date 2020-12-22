package app

import zio._
import zio.duration._
import zio.logging._
import app.models.Error.CoppeliaError

object App extends zio.App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    Booking.bookingCycle
      .catchSome({ case e: CoppeliaError => log.error(e.message) })
      .repeat(Schedule.fixed(5.minutes).jittered(1.0, 2.0))
      .provideCustomLayer(AppEnv.live)
      .exitCode

}
