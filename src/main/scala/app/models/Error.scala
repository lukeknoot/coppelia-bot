package app.models

object Error {

  sealed trait CoppeliaError { e: Exception =>
    def getExMessage = getMessage()
    def getExCause   = getCause()
  }

  case class ErrorHTTPResponse(message: String) extends Exception(message) with CoppeliaError

  case object OutOfVouchersException extends Exception("Ran out of vouchers") with CoppeliaError

}
