package app

import zio._
import zio.duration._
import zio.logging._

object App extends zio.App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    Booking.bookingCycle
      .catchAll(e => log.info(e.toString))
      .repeat(Schedule.fixed(1.minute).jittered(1.0, 2.0))
      .provideLayer(AppEnv.live)
      .exitCode

}
