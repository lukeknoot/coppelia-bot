package app.models

package app

import zio.test._
import Assertion._

object DanceClassTest extends DefaultRunnableSpec {

  val exampleClassFromAPI = DanceClass(
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

  def spec = suite("DanceClass")(
    test("isDesirable returns true if teacher and date is correct") {
      assert(exampleClassFromAPI.isDesirable)(equalTo(true))
    },
    test("isDesirable returns false if teacher is not correct") {
      val someoneElse = Staff("-1", "Someone Else")
      assert(exampleClassFromAPI.copy(staff = someoneElse).isDesirable)(equalTo(false))
    },
    test("isDesirable returns false if date is not correct") {
      val monday = "2020-12-07 00:00:00"
      assert(exampleClassFromAPI.copy(start = monday).isDesirable)(equalTo(false))
    },
    test("bookable returns true if canBook is true") {
      val bookableClass = exampleClassFromAPI
        .copy(
          canBook = true,
          available = "0",
          waitlist = "0"
        )
      assert(bookableClass.bookable)(equalTo(true))
    },
    test("bookable returns true if available is 1") {
      val bookableClass = exampleClassFromAPI
        .copy(
          canBook = false,
          available = "1",
          waitlist = "0"
        )
      assert(bookableClass.bookable)(equalTo(true))
    },
    test("bookable returns true if waitlist is 1") {
      val bookableClass = exampleClassFromAPI
        .copy(
          canBook = false,
          available = "0",
          waitlist = "1"
        )
      assert(bookableClass.bookable)(equalTo(true))
    }
  )
}
