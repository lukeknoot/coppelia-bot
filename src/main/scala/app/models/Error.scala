package app.models

object Error {

  case class ErrorHTTPResponse(s: String) extends Exception(s)

}
