package app.models

import app.parsing.Date
import sttp.model.CookieWithMeta
import java.util.Calendar
import java.util.TimeZone

case class Filter(
    date: List[String] = List(Filter.all),
    time: List[String] = List(Filter.all),
    classtype: List[String] = List(Filter.adultClasses),
    style: List[String] = List(Filter.ballet),
    level: List[String] = List(Filter.beginner),
    location: List[String] = List(Filter.all),
    keyword: List[String] = List(Filter.empty)
)

object Filter {
  val all          = "all"
  val empty        = ""
  val beginner     = "Beginner"
  val adultClasses = "Adult Classes"
  val ballet       = "Ballet"
}

case class Result(result: List[DanceClass])

case class DanceClass(
    id: String,
    title: String,
    start: String,
    scheduleID: String,
    waitlist: String,
    description: Description,
    canBook: Boolean,
    available: String,
    staff: Staff,
    location: Location,
    scheduleType: String
) {

  private def isTuesdayOrFriday: Boolean = {
    val calendar  = Calendar.getInstance(TimeZone.getTimeZone("Australia/Sydney"))
    calendar.setTimeInMillis(Date.parseClassDateStr(start))
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    dayOfWeek == 3 || dayOfWeek == 6
  }

  def isDesirable: Boolean = {
    staff.ID == Staff.alexandraRolfeID && isTuesdayOrFriday
  }

  def bookable: Boolean = canBook || available == "1" || waitlist == "1"
}

case class Description(name: String)

case class Location(ID: String)

case class AuthData(cookies: Seq[CookieWithMeta], authToken: String)

case class Staff(
    ID: String,
    name: String
)

object Staff {
  val alexandraRolfeID = "100000327"
}
