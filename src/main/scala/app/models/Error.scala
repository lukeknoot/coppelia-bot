package app.models

object Error {

  sealed trait CoppeliaError extends Throwable {
    def message: String
  }

  case class ErrorHTTPResponse(message: String) extends CoppeliaError

  case object OutOfVouchersException extends CoppeliaError {
    def message = "Ran out of vouchers"
  }

}
