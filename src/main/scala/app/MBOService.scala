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
import sttp.model.CookieWithMeta
import models.Error._

object MBOService {

  val live = ZLayer.succeed(Live.asInstanceOf[Service])

  type MBOService                    = Has[Service]
  type RAddToCart                    = AppState with Logging with SttpClient
  type RSignIn                       = AppState with Logging with SttpClient
  type RGetExistingBookingStartTimes = AppState with Logging with SttpClient
  type RCheckout                     = AppState with Logging with SttpClient

  trait Service {
    def addToCart(dc: DanceClass): RIO[RAddToCart, String]
    def signIn(user: String, pass: String): RIO[RSignIn, Unit]
    def getExistingBookingStartTimes: RIO[RGetExistingBookingStartTimes, List[String]]
    def checkout: RIO[RCheckout, String]
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
          .followRedirects(false)
      }

      for {
        _        <- log.info(s"Adding ${dc.toString} to cart")
        cookies  <- ZIO.accessM[AppState](_.get.get)
        request  <- ZIO.fromTry(getRequest(cookies))
        response <- send(request)
        body     <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse(_))
      } yield {
        body
      }
    }

    override def checkout = {

      def getRequest(cookies: Seq[CookieWithMeta]) = Try {
        basicRequest.get(uri"$checkoutURI").cookies(cookies).followRedirects(false)
      }

      for {
        _        <- log.info("Checking out")
        cookies  <- ZIO.accessM[AppState](_.get.get)
        request  <- ZIO.fromTry(getRequest(cookies))
        response <- send(request)
        body     <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse(_))
      } yield {
        body
      }
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
        response  <- send(basicRequest.get(uri"$signInPageURI"))
        htmlBody  <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse(_))
        authToken <- getAuthToken(htmlBody)
      } yield {
        AuthData(response.cookies, authToken)
      }

    override def signIn(
        username: String,
        password: String
    ) = {

      /** Sttp automatically converts Map[String, String] to
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

      for {
        _         <- log.info("Signing in")
        authData  <- loadAuthData
        _         <- log.info("Saving cookies")
        cookieRef <- ZIO.access[AppState](_.get)
        _         <- cookieRef.set(authData.cookies)
        request   <- ZIO.fromTry(getRequest(authData))
        _         <- send(request)
      } yield {
        ()
      }
    }

    override def getExistingBookingStartTimes = for {
      _          <- log.info("Getting existing bookings")
      cookies    <- ZIO.accessM[AppState](_.get.get)
      request    <- ZIO.fromTry(Try(basicRequest.get(uri"$scheduleURI").cookies(cookies)))
      response   <- send(request)
      body       <- ZIO.fromEither(response.body).mapError(ErrorHTTPResponse(_))
      startTimes <- Schedule.getStartTimes(body)
      _          <- if (startTimes.nonEmpty)
                      log.info(s"Found existing bookings for times: ${startTimes.toString()}")
                    else
                      log.info("Found no existing bookings")
    } yield {
      startTimes
    }

  }

}
