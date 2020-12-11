package app

import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.logging.Logging
import zio._
import test._
import Assertion._

object BookingTest extends DefaultRunnableSpec with TestUtil {

  val baseLayer =
    ZLayer.succeed(mockSDCServiceWithClasses) ++
      ZLayer.succeed(mockMBOService) ++
      AppState.live ++
      HttpClientZioBackend.stubLayer ++
      Logging.ignore ++
      mockConfig

  override def spec = suite("Booking cycle")(
    testM("Sends message to telegram after successful booking") {
      val bookingMessage = "Successfully booked classes:" +
        "\n- Ballet on Tuesday 2020-12-08 08:15:00 with Alexandra Rolfe (confirmed)"
      for {
        _ <- setNowTo("2020-12-08 00:00:00")
        r <- zio.Ref.make[Option[String]](None)
        l  = ZLayer.succeed(mockTelegramService(r))
        _ <- Booking.bookingCycle.provideCustomLayer(
               baseLayer ++ l ++ ZLayer.succeed(mockSDCServiceWithClasses)
             )
        v <- r.get
      } yield {
        assert(v)(
          Assertion.isSome(equalTo(bookingMessage))
        )
      }
    },
    testM("Does not send a message to telegram if no booking") {
      for {
        r <- zio.Ref.make[Option[String]](None)
        t  = ZLayer.succeed(mockTelegramService(r))
        _ <- Booking.bookingCycle.provideCustomLayer(
               t ++ baseLayer ++ ZLayer.succeed(mockSDCServiceNoClasses)
             )
        v <- r.get
      } yield {
        assert(v)(isNone)
      }
    }
  )
}
