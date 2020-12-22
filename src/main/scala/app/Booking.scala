package app

import app.models.DanceClass
import zio._
import zio.clock._
import parsing.Date
import zio.logging._
import zio.duration._

import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.text.SimpleDateFormat
import AppState.AppState
import models.Error.OutOfVouchersException
import app.models.Error.CoppeliaError

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

  private def toReadableString(dc: DanceClass, showState: Boolean = true): String = {
    val calendar   = Calendar.getInstance(Date.defaultTimeZone)
    calendar.setTimeInMillis(Date.parseClassDateStr(dc.start))
    val dateFormat = new SimpleDateFormat("EEEEE");
    dateFormat.setTimeZone(Date.defaultTimeZone)
    val dayOfWeek  = dateFormat.format(calendar.getTime());

    val state =
      if (dc.waitlist == "1") "waitlist"
      else if (dc.canBook || dc.available == "1") "confirmed"
      else "invalid state - double check booking"

    val stateStr = if (showState) s"($state)" else ""

    s"${dc.title} on $dayOfWeek ${dc.start} with ${dc.staff.name} ${stateStr}"
  }

  private def getBookingSuccessMessage(booked: List[DanceClass]): String =
    s"""Successfully booked classes:${booked
      .map(dc => s"\n- ${toReadableString(dc)}")
      .mkString}"""

  private def getBookingFailureMessage(toBook: List[DanceClass], e: CoppeliaError): String =
    s"""Couldn't book classes:${toBook
      .map(dc => s"\n- ${toReadableString(dc, showState = false)}")
      .mkString}
      |
      |${e.getMessage()}
      """.stripMargin

  def notifyOnce(message: String) = for {
    stateRef     <- ZIO.access[AppState](_.get)
    haveNotified <- stateRef.get.map(_.notifiedForVoucher)
    _            <- if (!haveNotified) {
                      TelegramService.sendMessage(message) *> setNotifiedStatus(true)
                    } else {
                      ZIO.succeed(())
                    }
  } yield {
    ()
  }

  def setNotifiedStatus(b: Boolean) = for {
    stateRef <- ZIO.access[AppState](_.get)
    state    <- stateRef.get
    _        <- stateRef.set(state.copy(notifiedForVoucher = b))
  } yield {
    ()
  }

  val bookingCycle = for {
    config           <- Config.loadConfig
    classes          <- SDCService.getClasses
    skipSignIn       <- Cookie.hasValidCookie
    _                <- if (skipSignIn) log.info("Have existing cookie, skipping sign in.")
                        else MBOService.signIn(config.mbo.username, config.mbo.password)
    existingBookings <- MBOService.getExistingBookingStartTimes
    toBook           <- filterClassesToBook(existingBookings, classes)
    notified         <- ZIO.accessM[AppState](_.get.map(_.notifiedForVoucher).get)
    _                <- if (toBook.nonEmpty) {
                          for {
                            _ <- log.info(s"Found ${toBook.length} classes to book")
                            _ <- ZIO.foreach_(toBook)(c => MBOService.addToCart(c))
                            _ <- ZIO.sleep(5.seconds)
                            _ <- MBOService.checkout.catchSome({ case e: OutOfVouchersException.type =>
                                   notifyOnce(getBookingFailureMessage(toBook, e)) *> ZIO.fail(e)
                                 })
                            _ <-
                              TelegramService.sendMessage(getBookingSuccessMessage(toBook)) *>
                                setNotifiedStatus(true)
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
