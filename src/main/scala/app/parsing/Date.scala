package app.parsing

import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Calendar

object Date {

  val defaultTimeZone = TimeZone.getTimeZone("Australia/Sydney")

  def parseClassDateStr(dateStr: String): Long = {
    val format   = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss"
    );
    format.setTimeZone(defaultTimeZone)
    val calendar = Calendar.getInstance(defaultTimeZone)
    calendar.setTime(format.parse(dateStr))
    calendar.getTimeInMillis()
  }

  def parseScheduleDateStr(dateStr: String): Long = {
    val format   = new SimpleDateFormat(
      "MM/dd/yyyy hh:mm a"
    );
    format.setTimeZone(defaultTimeZone)
    val calendar = Calendar.getInstance(defaultTimeZone)
    calendar.setTime(format.parse(dateStr))
    calendar.getTimeInMillis()
  }

  def isMoreThanEqual5HoursAgo(d1: Long, d2: Long): Boolean = {
    val fiveHoursMillis = 1000 * 60 * 60 * 5
    (d2 - d1) >= fiveHoursMillis
  }

}
