package app

import app.models.DanceClass
import zio._
import zio.clock._
import parsing.Date
import zio.logging._

import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat

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

  private def toReadableString(dc: DanceClass): String = {
    val calendar   = Calendar.getInstance(TimeZone.getTimeZone("Australia/Sydney"))
    calendar.setTimeInMillis(Date.parseClassDateStr(dc.start))
    val dateFormat = new SimpleDateFormat("EEEEE");
    val dayOfWeek  = dateFormat.format(calendar.getTime());

    val state =
      if (dc.waitlist == "1") "waitlist"
      else if (dc.canBook || dc.available == "1") "confirmed"
      else "invalid state - double check booking"

    s"${dc.title} on $dayOfWeek ${dc.start} with ${dc.staff.name} ($state)"
  }

  private def getBookingSuccessMessage(booked: List[DanceClass]): String =
    s"""Successfully booked classes:${booked
      .map(dc => s"\n- ${toReadableString(dc)}")
      .mkString}"""

  val bookingCycle = for {
    config           <- Config.loadConfig
    classes          <- SDCService.getClasses
    skipSignIn       <- Cookie.hasValidCookie
    _                <- if (skipSignIn) log.info("Have existing cookie, skipping sign in.")
                        else MBOService.signIn(config.mbo.username, config.mbo.password)
    existingBookings <- MBOService.getExistingBookingStartTimes
    toBook           <- filterClassesToBook(existingBookings, classes)
    _                <- if (toBook.nonEmpty) {
                          for {
                            _ <- log.info(s"Found ${toBook.length} classes to book")
                            _ <- ZIO.foreach_(toBook)(c => MBOService.addToCart(c))
                            _ <- MBOService.checkout
                            _ <- TelegramService.sendMessage(getBookingSuccessMessage(toBook))
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
