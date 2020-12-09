package app

import app.models.{DanceClass, Staff}
import zio._
import zio.clock._
import parsing.Date
import zio.logging._

import java.util.concurrent.TimeUnit

object Booking {

  type StartTime = String

  private def filterClassesToBook(
      existingBookings: List[StartTime],
      potentialClasses: List[DanceClass]
  ): URIO[Clock, List[DanceClass]] =
    currentTime(TimeUnit.MILLISECONDS).map { now =>
      val existingBookingTimes = existingBookings.map(Date.parseScheduleDateStr)
      potentialClasses.filter(c => {
        val classStartTime = Date.parseClassDateStr(c.start)

        c.isDesirable &&
        c.bookable &&
        !existingBookingTimes.contains(classStartTime) &&
        Date.isMoreThanEqual5HoursAway(now, classStartTime)
      })
    }

  val bookingCycle = for {
    config           <- Config.loadConfig
    classes          <- SDCService.getClasses
    skipSignIn       <- Cookie.hasValidCookie
    _                <- if (skipSignIn) log.info("Have existing cookie, skipping sign in.")
                        else MBOService.signIn(config.mboUsername, config.mboPassword)
    existingBookings <- MBOService.getExistingBookingStartTimes
    toBook           <- filterClassesToBook(existingBookings, classes)
    _                <- if (toBook.nonEmpty) {
                          for {
                            _ <- log.info(s"Found ${toBook.length} classes to book")
                            _ <- ZIO.foreach_(toBook)(c => MBOService.addToCart(c))
                            _ <- MBOService.checkout
                          } yield {
                            ()
                          }
                        } else {
                          log.info("Found no valid classes to book")
                        }
  } yield {
    ()
  }
}