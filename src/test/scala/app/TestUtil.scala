package app

import zio._
import scala.io.Source
import app.models._
import java.text.SimpleDateFormat
import java.time.Duration
import zio.test.environment.TestClock
import java.util.TimeZone
import _root_.app.models.Error.OutOfVouchersException

trait TestUtil {
  def managedResource(resourceName: String): ZManaged[Any, Nothing, String] = ZManaged
    .make {
      ZIO.effectTotal(Source.fromResource(resourceName))
    } { source => ZIO.effectTotal(source.close) }
    .mapM { source =>
      ZIO.effectTotal(source.getLines.mkString)
    }

  def setNowTo(timeStr: String): zio.URIO[TestClock, Unit] = {
    val sdf  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    sdf.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"))
    val date = sdf.parse(timeStr);
    TestClock.setTime(Duration.ofMillis(date.toInstant().toEpochMilli))
  }

  val desiredClass = DanceClass(
    id = "57414",
    title = "Ballet",
    start = "2020-12-08 08:15:00",
    scheduleID = "2686",
    waitlist = "0",
    description = Description("Ballet"),
    canBook = false,
    available = "0",
    staff = Staff("100000327", "Alexandra Rolfe"),
    location = Location("1"),
    scheduleType = "Class"
  )

  def mockTelegramService(ref: Ref[Option[String]]) = new TelegramService.Service {
    override def sendMessage(m: String): RIO[TelegramService.RSendMessage, Unit] = {
      for {
        _ <- ref.set(Some(m))
      } yield {
        ()
      }
    }
  }

  def mockMBOService = new MBOService.Service {
    override def addToCart(dc: DanceClass): RIO[MBOService.RAddToCart, Unit] = ZIO.succeed(())

    override def signIn(user: String, pass: String): RIO[MBOService.RSignIn, Unit] = ZIO.succeed(())

    override def getExistingBookingStartTimes
        : RIO[MBOService.RGetExistingBookingStartTimes, List[String]] = ZIO.succeed(List())

    override def checkout: RIO[MBOService.RCheckout, Unit] = ZIO.succeed(())
  }

  def mockMBOServiceNoVouchers = new MBOService.Service {
    override def addToCart(dc: DanceClass): RIO[MBOService.RAddToCart, Unit] = ZIO.succeed(())

    override def signIn(user: String, pass: String): RIO[MBOService.RSignIn, Unit] = ZIO.succeed(())

    override def getExistingBookingStartTimes
        : RIO[MBOService.RGetExistingBookingStartTimes, List[String]] = ZIO.succeed(List())

    override def checkout: RIO[MBOService.RCheckout, Unit] = ZIO.fail(OutOfVouchersException)
  }

  val mockSDCServiceWithClasses: SDCService.Service = new SDCService.Service {
    override def getClasses: zio.RIO[SDCService.RGetClasses, List[DanceClass]] =
      ZIO.succeed(List(desiredClass.copy(canBook = true, start = "2020-12-08 08:15:00")))
  }

  val mockSDCServiceNoClasses: SDCService.Service = new SDCService.Service {
    override def getClasses: zio.RIO[SDCService.RGetClasses, List[DanceClass]] =
      ZIO.succeed(List(desiredClass.copy(canBook = false, start = "2020-12-08 08:15:00")))
  }

  val mockConfig = ZLayer.succeed(Config(MBOConfig("", ""), TelegramConfig("", "")))
}
