package app

import zio.test._
import Assertion._
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.client3.httpclient.zio.stubbing._
import sttp.model._
import sttp.client3._
import zio.logging.Logging
import zio.ZIO
import AppState.AppState

object MBOServiceTest extends DefaultRunnableSpec with TestUtil {

  override val spec = suite("MBOService")(
    testM("Retrieves start time from schedule") {
      val httpStub = managedResource("schedule-mock.html").use(data => {
        whenRequestMatchesPartial {
          case r
              if r.method == Method.GET && r.uri.toString
                .startsWith(MBOService.Live.scheduleURI) =>
            Response.ok(data)
        }
      })
      (for {
        _          <- httpStub
        startTimes <- MBOService.getExistingBookingStartTimes
      } yield {
        assert(startTimes)(equalTo(List("12/1/2020 5:30 PM")))
      })
    },
    testM("Retrieves multiple start times for schedule") {
      val httpStub = managedResource("schedule-mock-multiple.html").use(data => {
        whenRequestMatchesPartial {
          case r
              if r.method == Method.GET && r.uri.toString
                .startsWith(MBOService.Live.scheduleURI) =>
            Response.ok(data)
        }
      })
      for {
        _          <- httpStub
        startTimes <- MBOService.getExistingBookingStartTimes
      } yield assert(startTimes)(
        equalTo(
          List(
            "12/8/2020 5:30 PM",
            "12/11/2020 9:00 AM",
            "12/11/2020 8:15 PM"
          )
        )
      )
    },
    testM("Ssaves cookies if sign-in succeeds") {
      val initialSignInPageStub = whenRequestMatches(r =>
        r.uri.toString.startsWith(MBOService.Live.signInPageURI) && r.method == Method.GET
      ).thenRespond(
        Response(
          "",
          StatusCode.Ok,
          "OK",
          Seq(Header.setCookie(CookieWithMeta("test-cookie", "value")))
        )
      )
      val signInPostStub        = whenRequestMatches(r =>
        r.uri.toString.startsWith(MBOService.Live.signInURI) && r.method == Method.POST
      ).thenRespondOk
      for {
        _       <- initialSignInPageStub
        _       <- signInPostStub
        _       <- MBOService.signIn("", "")
        cookies <- ZIO.accessM[AppState](_.get.get)
      } yield assert(cookies)(equalTo(Seq(CookieWithMeta("test-cookie", "value"))))
    },
    testM("Fails if schedule loading times out") {
      val httpStub = managedResource("schedule-mock-timeout.html").use(data => {
        whenRequestMatches { r =>
          r.method == Method.GET && r.uri.toString.startsWith(MBOService.Live.scheduleURI)
        }.thenRespond(Response.ok(data))
      })
      httpStub *>
        MBOService.getExistingBookingStartTimes.fold(
          e => assert(e)(hasMessage(containsString("Couldn't load schedule"))),
          a => assert(true)(isFalse)
        )
    },
    testM("Fails if sign-in fails") {
      val initialSignInPageStub = whenRequestMatches(r =>
        r.uri.toString.startsWith(MBOService.Live.signInPageURI) && r.method == Method.GET
      ).thenRespondOk

      val signInPostStub = managedResource("failed-signin.html").use(data => {
        whenRequestMatches(r =>
          r.uri.toString.startsWith(MBOService.Live.signInURI) && r.method == Method.POST
        ).thenRespond(Response.ok(data))
      })

      for {
        _ <- initialSignInPageStub
        _ <- signInPostStub
        r <- MBOService.signIn("", "").run
      } yield {
        assert(r)(fails(hasMessage(containsString("Failed to sign-in"))))
      }
    },
    testM("Wipes cookies if sign-in fails") {
      val initialSignInPageStub = whenRequestMatches(r =>
        r.uri.toString.startsWith(MBOService.Live.signInPageURI) && r.method == Method.GET
      ).thenRespond(
        Response(
          "",
          StatusCode.Ok,
          "OK",
          Seq(Header.setCookie(CookieWithMeta("test-cookie", "value")))
        )
      )

      val signInPostStub = managedResource("failed-signin.html").use(data => {
        whenRequestMatches(r =>
          r.uri.toString.startsWith(MBOService.Live.signInURI) && r.method == Method.POST
        ).thenRespond(Response.ok(data))
      })

      for {
        _       <- initialSignInPageStub
        _       <- signInPostStub
        _       <- MBOService.signIn("", "").run
        cookies <- ZIO.accessM[AppState](_.get.get)
      } yield {
        assert(cookies)(isEmpty)
      }
    }
  ).provideLayer(
    MBOService.live ++ AppState.live ++ HttpClientZioBackend.stubLayer ++ Logging.ignore
  )
  // TODO test sign in submits auth token, but can't with ZIO sttp backend
  // because can't intercept request?
}
