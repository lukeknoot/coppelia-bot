package app.models

object Error {

  sealed trait CoppeliaError { e: Exception =>
    def getExMessage = getMessage()
    def getExCause   = getCause()
  }

  /**
   * Represents other types of HTTP client failures
   */
  case class ErrorHTTPNetwork(t: Throwable) extends Exception(t) with CoppeliaError

  /**
   * Represents 4xx and 5xx HTTP responses.
   */
  case class ErrorHTTPResponse(message: String) extends Exception(message) with CoppeliaError

  case object OutOfVouchersException extends Exception("Ran out of vouchers") with CoppeliaError

}
