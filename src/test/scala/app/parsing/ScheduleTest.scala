package app.parsing

import zio.test._
import app.parsing.Schedule
import Assertion._
import zio.ZManaged
import zio.ZIO
import scala.io.Source
import app.TestUtil

object ScheduleTest extends DefaultRunnableSpec with TestUtil {

  def spec = suite("Schedule")(
    testM("getStartTimes extracts a class start time from HTML") {
      for {
        startTimes <- managedResource("schedule-mock.html")
                        .use(html => Schedule.getStartTimes(html))
      } yield assert(startTimes)(equalTo(List("12/1/2020 5:30 PM")))
    },
    testM("getStartTimes extracts multiple class start times on multiple days from HTML") {
      for {
        startTimes <- managedResource("schedule-mock-multiple.html")
                        .use(html => Schedule.getStartTimes(html))
      } yield assert(startTimes)(
        equalTo(
          List(
            "12/8/2020 5:30 PM",
            "12/11/2020 9:00 AM",
            "12/11/2020 8:15 PM"
          )
        )
      )
    }
  )

}
