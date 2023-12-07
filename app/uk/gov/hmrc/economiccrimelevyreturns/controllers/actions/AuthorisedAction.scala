/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import cats.data.EitherT
import com.google.inject.Inject
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.AuthorisationError
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.NrsIdentityData
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import java.lang
import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with BackendHeaderCarrierProvider
    with ActionFunction[Request, AuthorisedRequest]

class BaseAuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedAction
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions {

  val retrievals = Retrievals.externalId and Retrievals.confidenceLevel and
    Retrievals.nino and Retrievals.saUtr and Retrievals.mdtpInformation and Retrievals.credentialStrength and
    Retrievals.loginTimes and Retrievals.credentials and Retrievals.name and Retrievals.dateOfBirth and Retrievals.email and
    Retrievals.affinityGroup and Retrievals.agentCode and Retrievals.agentInformation and Retrievals.credentialRole and
    Retrievals.groupIdentifier and Retrievals.itmpName and Retrievals.itmpDateOfBirth and Retrievals.itmpAddress

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {
    val authorisedFunction = authorised(Enrolment(EclEnrolment.ServiceName))

    for {
      eclReference   <- getEclRegistrationReference(authorisedFunction, request)
      internalId     <- getInternalId(authorisedFunction, request)
      nrsIdentityData = generateNrsIdentityData(internalId, authorisedFunction, request)

    } yield nrsIdentityData

  }

//      authorised(Enrolment(EclEnrolment.ServiceName)).retrieve(retrievals) {
//      case optInternalId ~ enrolments ~ optExternalId ~ confidenceLevel ~ optNino ~ optSaUtr ~
//          optMdtpInformation ~ optCredentialStrength ~ loginTimes ~ optCredentials ~ optName ~ optDateOfBirth ~
//          optEmail ~ optAffinityGroup ~ optAgentCode ~ agentInformation ~ optCredentialRole ~ optGroupIdentifier ~
//          optItmpName ~ optItmpDateOfBirth ~ optItmpAddress =>
//        val internalId = optInternalId.getOrElseFail("Unable to retrieve internalId")
//
//        val eclRegistrationReference =
//          enrolments
//            .getEnrolment(EclEnrolment.ServiceName)
//            .flatMap(_.getIdentifier(EclEnrolment.IdentifierKey))
//            .getOrElseFail(
//              s"Unable to retrieve enrolment with key ${EclEnrolment.ServiceName} and identifier ${EclEnrolment.IdentifierKey}"
//            )
//            .value
//
//        val nrsIdentityData = NrsIdentityData(
//          internalId,
//          optExternalId,
//          optAgentCode,
//          optCredentials,
//          confidenceLevel.level,
//          optNino,
//          optSaUtr,
//          optName,
//          optDateOfBirth,
//          optEmail,
//          agentInformation,
//          optGroupIdentifier,
//          optCredentialRole,
//          optMdtpInformation,
//          optItmpName,
//          optItmpDateOfBirth,
//          optItmpAddress,
//          optAffinityGroup,
//          optCredentialStrength,
//          loginTimes
//        )
//
//        block(AuthorisedRequest(request, internalId, eclRegistrationReference, nrsIdentityData))
//
//    }(hc(request), executionContext) recover { case e: AuthorisationException =>
//      Unauthorized(
//        Json.toJson(
//          ErrorResponse(
//            UNAUTHORIZED,
//            e.reason
//          )
//        )
//      )
//    }

  def getEclRegistrationReference(authorisedFunction: AuthorisedFunction, request: Request[_]) =
    EitherT {
      authorisedFunction
        .retrieve(Retrievals.authorisedEnrolments) { case enrolments =>
          enrolments
            .getEnrolment(EclEnrolment.ServiceName)
            .flatMap(_.getIdentifier(EclEnrolment.IdentifierKey)) match {
            case Some(eclReference) => Future.successful(Right(eclReference.value))
            case None               => Future.successful(Left(AuthorisationError.InternalUnexpectedError(None)))
          }
        }(hc(request), executionContext)
        .recover {
          convertToAuthorisationError
        }
    }

  def getInternalId(
    authorisedFunction: AuthorisedFunction,
    request: Request[_]
  ): EitherT[Future, AuthorisationError, String] =
    EitherT {
      authorisedFunction
        .retrieve(Retrievals.internalId) {
          case Some(internalId) => Future.successful(Right(internalId))
          case None             => Future.successful(Left(AuthorisationError.InternalUnexpectedError(None)))
        }(hc(request), executionContext)
        .recover {
          convertToAuthorisationError
        }
    }

  val convertToAuthorisationError: PartialFunction[Throwable, Either[AuthorisationError, String]] = {
    case e: AuthorisationException =>
      Left(AuthorisationError.Unauthorized(e.reason, Some(e)))
    case e                         => Left(AuthorisationError.InternalUnexpectedError(Some(e)))
  }

  def generateNrsIdentityData(internalId: String, authorisedFunction: AuthorisedFunction, request: Request[_]) =
    authorisedFunction.retrieve(retrievals) {
      case optExternalId ~ confidenceLevel ~ optNino ~ optSaUtr ~
          optMdtpInformation ~ optCredentialStrength ~ loginTimes ~ optCredentials ~ optName ~ optDateOfBirth ~
          optEmail ~ optAffinityGroup ~ optAgentCode ~ agentInformation ~ optCredentialRole ~ optGroupIdentifier ~
          optItmpName ~ optItmpDateOfBirth ~ optItmpAddress =>
        Future.successful(
          NrsIdentityData(
            internalId,
            optExternalId,
            optAgentCode,
            optCredentials,
            confidenceLevel.level,
            optNino,
            optSaUtr,
            optName,
            optDateOfBirth,
            optEmail,
            agentInformation,
            optGroupIdentifier,
            optCredentialRole,
            optMdtpInformation,
            optItmpName,
            optItmpDateOfBirth,
            optItmpAddress,
            optAffinityGroup,
            optCredentialStrength,
            loginTimes
          )
        )
    }(hc(request), executionContext)

  def getAuthorisedRequest(internalId: String, eclReference: String, nrsIdentityData: NrsIdentityData) = {}

//  def getAuthorisedRequest(
//    eclReference: String,
//    request: Request[_]
//  ): EitherT[Future, AuthorisationError, AuthorisedRequest] =
//    EitherT {
//      authorised(Enrolment(EclEnrolment.ServiceName)).retrieve(retrievals) {
//        case Some(internalId) ~ enrolments ~ optExternalId ~ confidenceLevel ~ optNino ~ optSaUtr ~
//            optMdtpInformation ~ optCredentialStrength ~ loginTimes ~ optCredentials ~ optName ~ optDateOfBirth ~
//            optEmail ~ optAffinityGroup ~ optAgentCode ~ agentInformation ~ optCredentialRole ~ optGroupIdentifier ~
//            optItmpName ~ optItmpDateOfBirth ~ optItmpAddress =>
//
//          val nrsIdentityData = NrsIdentityData(
//            internalId,
//            optExternalId,
//            optAgentCode,
//            optCredentials,
//            confidenceLevel.level,
//            optNino,
//            optSaUtr,
//            optName,
//            optDateOfBirth,
//            optEmail,
//            agentInformation,
//            optGroupIdentifier,
//            optCredentialRole,
//            optMdtpInformation,
//            optItmpName,
//            optItmpDateOfBirth,
//            optItmpAddress,
//            optAffinityGroup,
//            optCredentialStrength,
//            loginTimes
//          )
//          Future.successful(Right(AuthorisedRequest(request, internalId, eclReference, nrsIdentityData)))
//        case _ =>
//          Future.successful(Left(AuthorisationError.InternalUnexpectedError(None)))
//      }(hc(request), executionContext)
//    }

  implicit class OptionOps[T](o: Option[T]) {
    def getOrElseFail(failureMessage: String): T = o.getOrElse(throw new IllegalStateException(failureMessage))
  }

}
