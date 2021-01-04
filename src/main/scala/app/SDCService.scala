package app

import zio._
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3._
import sttp.client3.httpclient.zio._
import io.circe.parser.parse
import io.circe.Error
import sttp.model.Header
import models._
import models.Error._
import sttp.model.MediaType
import zio.logging._
import scala.util.Try

object SDCService {

  val live = ZLayer.succeed(Live.asInstanceOf[Service])

  type SDCService  = Has[Service]
  type RGetClasses = Logging with SttpClient

  trait Service {
    def getClasses: RIO[RGetClasses, List[DanceClass]]
  }

  val service = ZIO.service[Service]

  def getClasses = service.flatMap(_.getClasses)

  object Live extends Service {

    val baseURI = "https://www.sydneydancecompany.com"

    private def parseClassJson(jsonStr: String): Either[Error, Result] = {
      val parsed = parse(jsonStr)
      parsed.flatMap(_.as[Result])
    }

    override def getClasses = {

      val classListURI = s"$baseURI/json-api/calendar/classes"

      val classesRequest = Try {
        basicRequest
          .post(uri"$classListURI")
          .headers(Header.contentType(MediaType.ApplicationJson))
          .body(Filter().asJson.toString)
      }

      for {
        request  <- ZIO.fromTry(classesRequest)
        _        <- log.info("Retrieving classes")
        response <- send(request).mapError(new ErrorHTTPNetwork(_))
        body     <- ZIO.fromEither(response.body).mapError(new ErrorHTTPResponse(_))
        parsed   <- ZIO.fromEither(parseClassJson(body))
      } yield {
        parsed.result
      }
    }

  }
}
