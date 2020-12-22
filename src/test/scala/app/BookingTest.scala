package app

import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.logging._
import zio._
import test._
import Assertion._
import AppState.AppState
import zio.test.environment.TestClock
import zio.duration._

object BookingTest extends DefaultRunnableSpec with TestUtil {

  val baseLayer =
    ZLayer.succeed(mockMBOService) ++
      Logging.ignore ++
      mockConfig ++
      HttpClientZioBackend.stubLayer

  override def spec = suite("Booking cycle")(
    testM("Sends message to telegram after successful booking") {
      val bookingMessage = "Successfully booked classes:" +
        "\n- Ballet on Tuesday 2020-12-08 08:15:00 with Alexandra Rolfe (confirmed)"
      for {
        _      <- setNowTo("2020-12-08 00:00:00")
        ref    <- zio.Ref.make[Option[String]](None)
        t       = ZLayer.succeed(mockTelegramService(ref))
        res    <- Booking.bookingCycle
                    .provideSomeLayer[ZEnv with AppState](
                      baseLayer ++ t ++ ZLayer.succeed(mockSDCServiceWithClasses)
                    )
                    .fork
        _      <- TestClock.adjust(10.seconds)
        _      <- res.join
        refVal <- ref.get
      } yield {
        assert(refVal)(
          Assertion.isSome(equalTo(bookingMessage))
        )
      }
    }.provideCustomLayer(AppState.live),
    testM("Does not send a message to telegram if no booking") {
      for {
        ref    <- zio.Ref.make[Option[String]](None)
        t       = ZLayer.succeed(mockTelegramService(ref))
        _      <- Booking.bookingCycle.provideSomeLayer[ZEnv with AppState](
                    baseLayer ++ t ++ ZLayer.succeed(mockSDCServiceNoClasses)
                  )
        refVal <- ref.get
      } yield {
        assert(refVal)(isNone)
      }
    }.provideCustomLayer(AppState.live),
    testM("Sends only 1 failure message if ran out of vouchers") {
      for {
        ref           <- zio.Ref.make[Option[String]](None)
        t              = ZLayer.succeed(mockTelegramService(ref))
        res1          <- Booking.bookingCycle
                           .provideSomeLayer[ZEnv with AppState](
                             baseLayer ++ t ++ ZLayer.succeed(mockSDCServiceWithClasses) ++ ZLayer.succeed(
                               mockMBOServiceNoVouchers
                             )
                           )
                           .ignore
                           .fork
        _             <- TestClock.adjust(10.seconds)
        _             <- res1.join
        firstMessage  <- ref.get.map(s => assert(s)(isSome(isNonEmptyString)))
        _             <- ref.set(None)
        res2          <- Booking.bookingCycle
                           .provideSomeLayer[ZEnv with AppState](
                             baseLayer ++ t ++ ZLayer.succeed(mockSDCServiceWithClasses) ++ ZLayer.succeed(
                               mockMBOServiceNoVouchers
                             )
                           )
                           .ignore
                           .fork
        _             <- TestClock.adjust(10.seconds)
        _             <- res2.join
        secondMessage <- ref.get.map(s => assert(s)(isNone))
      } yield {
        firstMessage && secondMessage
      }
    }.provideCustomLayer(AppState.live)
  )
}
