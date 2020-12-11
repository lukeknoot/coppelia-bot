package app

import models._
import zio.test._
import Assertion._
import sttp.model._
import sttp.client3._
import sttp.client3.httpclient.zio._
import sttp.client3.httpclient.zio.stubbing._
import zio.logging._

object SDCServiceTest extends DefaultRunnableSpec with TestUtil {

  def stub(data: String) = whenRequestMatches(r =>
    r.method == Method.POST && r.uri.toString.startsWith(SDCService.Live.baseURI)
  ).thenRespond(Response.ok(data))

  override def spec = suite("SDCService") {
    testM("Parses classes from API") {
      val exampleClassFromAPI = DanceClass(
        id = "57414",
        title = "Ballet",
        start = "2020-11-28 08:15:00",
        scheduleID = "2686",
        waitlist = "0",
        description = Description("Ballet"),
        canBook = false,
        available = "0",
        staff = Staff("100000327", "Alexandra Rolfe"),
        location = Location("1"),
        scheduleType = "Class"
      )
      for {
        _       <- managedResource("example-classes.json").use(d => stub(d))
        classes <- SDCService.getClasses
      } yield {
        assert(classes)(contains(exampleClassFromAPI))
      }
    }.provideLayer(SDCService.live ++ HttpClientZioBackend.stubLayer ++ Logging.ignore)
  }
}
