package app.parsing

import zio.test._
import app.parsing.Date
import Assertion._

object DateTest extends DefaultRunnableSpec {

  val millis           = 1605853800000L
  val twelvehourMillis = 1000 * 60 * 60 * 12

  def spec = suite("Date")(
    test("parseClassDateStr should parse date strings from class list") {
      assert(Date.parseClassDateStr("2020-11-20 17:30:00"))(equalTo(millis))
    },
    test("parseClassDateStr should parse date strings from class list ambiguous 12 hour") {
      assert(Date.parseClassDateStr("2020-11-20 5:30:00"))(equalTo(millis - twelvehourMillis))
    },
    test("parseScheduleDateStr should parse date strings from schedule list") {
      assert(Date.parseScheduleDateStr("11/20/2020 5:30 PM"))(equalTo(millis))
    },
    test("isMoreThanEqual5HoursAgo returns true if date is at least 5 hours ago") {
      val d1 = Date.parseClassDateStr("2020-11-20 12:30:00")
      val d2 = Date.parseClassDateStr("2020-11-20 17:30:00")
      assert(Date.isMoreThanEqual5HoursAway(d1, d2))(equalTo(true))
    },
    test("isMoreThanEqual5HoursAgo returns false if date is less than 5 hours ago") {
      val d1 = Date.parseClassDateStr("2020-11-20 12:30:01")
      val d2 = Date.parseClassDateStr("2020-11-20 17:30:00")
      assert(Date.isMoreThanEqual5HoursAway(d1, d2))(equalTo(false))
    }
  )

}
