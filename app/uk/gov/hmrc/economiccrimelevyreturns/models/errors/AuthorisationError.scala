package uk.gov.hmrc.economiccrimelevyreturns.models.errors

sealed trait AuthorisationError

object AuthorisationError {
  case class InternalUnexpectedError(cause: Option[Throwable]) extends AuthorisationError
  case class Unauthorized(reason: String, cause: Option[Throwable]) extends AuthorisationError
}
