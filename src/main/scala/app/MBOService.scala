package app

import zio._
import sttp.client3._
import org.jsoup.Jsoup
import scala.util.Try
import models._
import app.AppState.AppState
import zio.logging._
import sttp.client3.httpclient.zio._
import parsing.Schedule
import models.Error._
import sttp.model._

object MBOService {

  val live = ZLayer.succeed(Live.asInstanceOf[Service])

  type MBOService                    = Has[Service]
  type RAddToCart                    = AppState with Logging with SttpClient
  type RSignIn                       = AppState with Logging with SttpClient
  type RGetExistingBookingStartTimes = AppState with Logging with SttpClient
  type RCheckout                     = AppState with Logging with SttpClient

  trait Service {
    def addToCart(dc: DanceClass): RIO[RAddToCart, Unit]
    def signIn(user: String, pass: String): RIO[RSignIn, Unit]
    def getExistingBookingStartTimes: RIO[RGetExistingBookingStartTimes, List[String]]
    def checkout: RIO[RCheckout, Unit]
  }

  val service = ZIO.service[Service]

  def addToCart(dc: DanceClass)          = service.flatMap(_.addToCart(dc))
  def signIn(user: String, pass: String) = service.flatMap(_.signIn(user, pass))
  def getExistingBookingStartTimes       = service.flatMap(_.getExistingBookingStartTimes)
  def checkout                           = service.flatMap(_.checkout)

  object Live extends Service {

    val baseURI = "https://cart.mindbodyonline.com/sites/3173"

    val addToCartURI  = s"$baseURI/cart/add_booking"
    val signInPageURI = s"$baseURI/session/new"
    val checkoutURI   = s"$baseURI/cart/proceed_to_checkout"
    val signInURI     = s"$baseURI/session"
    val scheduleURI   = s"$baseURI/client/schedules"
    val buyVoucherURI = s"$baseURI/cart/select_service"

    override def addToCart(dc: DanceClass) = {
      val queryParams = Map(
        "item[class_schedule_id]" -> dc.scheduleID,
        "item[info]"              -> "", // Doesn't seem to be needed. Cryptic date string usually.
        "item[mbo_id]"            -> dc.id,
        "item[mbo_location_id]"   -> dc.location.ID,
        "item[name]"              -> dc.description.name,
        "item[type]"              -> dc.scheduleType
      )

      def getRequest(cookies: Seq[CookieWithMeta]) = Try {
        basicRequest
          .get(uri"$addToCartURI?$queryParams")
          .cookies(cookies)
      }

      for {
        _        <- log.info(s"Adding ${dc.toString} to cart")
        state    <- ZIO.accessM[AppState](_.get.get)
        request  <- ZIO.fromTry(getRequest(state.cookies))
        response <- send(request).mapError(new ErrorHTTPNetwork(_))
        body     <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse)
      } yield {
        ()
      }
    }

    override def checkout = {

      def failIfNoVouchers(r: Response[Either[String, String]]) = {
        val locationHeader = r.headers.find(_.is(HeaderNames.Location))
        if (r.code == StatusCode.Found && locationHeader.nonEmpty) {
          val uri = locationHeader.get.value
          if (uri == buyVoucherURI) {
            ZIO.fail(OutOfVouchersException)
          } else {
            ZIO.succeed(r)
          }
        } else {
          ZIO.succeed(r)
        }
      }

      def getRequest(cookies: Seq[CookieWithMeta]) = Try {
        basicRequest.get(uri"$checkoutURI").cookies(cookies).followRedirects(false)
      }

      for {
        _        <- log.info("Checking out")
        state    <- ZIO.accessM[AppState](_.get.get)
        request  <- ZIO.fromTry(getRequest(state.cookies))
        response <- send(request).mapError(new ErrorHTTPNetwork(_)) >>= failIfNoVouchers
        body     <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse)
      } yield {
        ()
      }
    }

    // STTP doesn't currently seem to redirect with cookies (?)
    // so we have some special handling here.
    private def redirectWithCookies(r: Response[Either[String, String]]) = {
      val locationHeader = r.headers.find(_.is(HeaderNames.Location))
      if (r.code == StatusCode.Found && locationHeader.nonEmpty) {
        val uri = locationHeader.get.value
        for {
          _        <- log.info("Redirecting with cookie")
          state    <- ZIO.accessM[AppState](_.get.get)
          request   = basicRequest
                        .get(uri"$uri")
                        .cookies(state.cookies)
          response <- send(request).mapError(new ErrorHTTPNetwork(_))
        } yield {
          response
        }
      } else ZIO.succeed(r)
    }

    private def getAuthToken(html: String): Task[String] = ZIO
      .fromTry(
        Try(
          Jsoup
            .parse(html)
            .select("form[id=new_mb_client_session]")
            .select("input[name=authenticity_token]")
            .attr("value")
        )
      )

    private def loadAuthData =
      for {
        _         <- log.info("Loading cookie and auth token for sign in")
        response  <- send(basicRequest.get(uri"$signInPageURI")).mapError(new ErrorHTTPNetwork(_))
        htmlBody  <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse)
        authToken <- getAuthToken(htmlBody)
      } yield {
        AuthData(response.cookies, authToken)
      }

    override def signIn(
        username: String,
        password: String
    ) = {

      /**
       * Sttp automatically converts Map[String, String] to
       * application/x-www-form-urlencoded form-data if supplies
       * as the body.
       */
      def getFormData(
          authToken: String,
          username: String,
          password: String
      ): Map[String, String] = {
        val usernameField  = "mb_client_session[username]"
        val passwordField  = "mb_client_session[password]"
        val authTokenField = "authenticity_token"
        Map(
          usernameField  -> username,
          passwordField  -> password,
          authTokenField -> authToken
        )
      }

      def getRequest(authData: AuthData) = Try {
        basicRequest
          .post(uri"$signInURI")
          .body(
            getFormData(
              authToken = authData.authToken,
              username = username,
              password = password
            )
          )
          .cookies(authData.cookies)
          .followRedirects(false)
      }

      def didFail(body: String) =
        body.contains("The email and password you entered don&#39;t match")

      for {
        _        <- log.info("Signing in")
        authData <- loadAuthData
        stateRef <- ZIO.access[AppState](_.get)
        _        <- log.info("Saving cookies")
        state    <- stateRef.get
        _        <- stateRef.set(state.copy(cookies = authData.cookies))
        request  <- ZIO.fromTry(getRequest(authData))
        response <- send(request).mapError(new ErrorHTTPNetwork(_)) >>= redirectWithCookies
        body     <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse)
        _        <- if (didFail(body))
                      ZIO
                        .fail(ErrorHTTPResponse("Failed to sign-in."))
                        .ensuring(log.info("Wiping cookies") *> stateRef.set(state.copy(cookies = Seq())))
                    else ZIO.succeed(())
      } yield {
        ()
      }
    }

    /**
     * TODO: Confirm timeout behaviour of site.
     *
     * Have only seen this once but seems like their page will
     * tell us if it's a timeout.
     */
    def failIfTimeout(htmlBody: String): RIO[Logging, Unit] = {
      for {
        textBody <- ZIO.fromTry(Try(Jsoup.parse(htmlBody).select("body").text()))
        _        <- if (textBody.contains("timeout") || textBody.contains("timed out")) {
                      ZIO.fail(ErrorHTTPResponse("Couldn't load schedule. Seems to have timed out."))
                    } else {
                      ZIO.succeed(())
                    }
      } yield {
        ()
      }
    }

    override def getExistingBookingStartTimes = for {
      _          <- log.info("Getting existing bookings")
      state      <- ZIO.accessM[AppState](_.get.get)
      request    <- ZIO.fromTry(Try(basicRequest.get(uri"$scheduleURI").cookies(state.cookies)))
      response   <- send(request).mapError(new ErrorHTTPNetwork(_))
      body       <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse)
      _          <- failIfTimeout(body)
      startTimes <- Schedule.getStartTimes(body)
      _          <- if (startTimes.nonEmpty)
                      log.info(s"Found existing bookings for times: ${startTimes.toString()}")
                    else log.info("Found no existing bookings")
    } yield {
      startTimes
    }

  }

}
