package app

import zio._
import sttp.client3.httpclient.zio._
import sttp.client3._
import models.Error._

object TelegramService {

  val service = ZIO.service[Service]

  object Method {
    val sendMessage = "sendMessage"
  }

  val live = ZLayer.succeed(Live.asInstanceOf[Service])

  def sendMessage(m: String) = service.flatMap(_.sendMessage(m))

  type TelegramService = Has[Service]

  type RSendMessage = SttpClient with Has[Config]

  trait Service {
    def sendMessage(m: String): RIO[RSendMessage, Unit]
  }

  object Live extends Service {
    val botURI = "https://api.telegram.org/bot"

    def methodURI(token: String, method: String): String = s"$botURI$token/$method"

    override def sendMessage(m: String): RIO[RSendMessage, Unit] = {
      for {
        token  <- ZIO.access[Has[Config]](_.get.telegram.token)
        chatID <- ZIO.access[Has[Config]](_.get.telegram.chatID)
        request = basicRequest
                    .post(uri"${methodURI(token, Method.sendMessage)}")
                    .body(Map("chat_id" -> chatID, "text" -> m))
        _      <- send(request).mapError(new ErrorHTTPNetwork(_))
      } yield {
        ()
      }
    }
  }

}
