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
import play.api.libs.json.Json
import play.api.mvc.Results.Status
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.NrsIdentityData
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

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

  private val nrsIdentityDataRetrievals = Retrievals.externalId and Retrievals.confidenceLevel and
    Retrievals.nino and Retrievals.saUtr and Retrievals.mdtpInformation and Retrievals.credentialStrength and
    Retrievals.loginTimes and Retrievals.credentials and Retrievals.name and Retrievals.dateOfBirth and Retrievals.email and
    Retrievals.affinityGroup and Retrievals.agentCode and Retrievals.agentInformation and Retrievals.credentialRole and
    Retrievals.groupIdentifier and Retrievals.itmpName and Retrievals.itmpDateOfBirth and Retrievals.itmpAddress

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {
    val authorisedFunction = authorised(Enrolment(EclEnrolment.serviceName))

    (for {
      eclReference     <- getEclRegistrationReference(authorisedFunction, request)
      internalId       <- getInternalId(authorisedFunction, request)
      nrsIdentityData  <- generateNrsIdentityData(internalId, authorisedFunction, request)
      authorisedRequest = AuthorisedRequest(request, internalId, eclReference, nrsIdentityData)
    } yield authorisedRequest).foldF(
      err => Future.successful(Status(err.code.statusCode)(Json.toJson(err))),
      response => block(response)
    )
  }

  private def convertToAuthorisationError[T]: PartialFunction[Throwable, Either[ResponseError, T]] = {
    case e: AuthorisationException =>
      Left(ResponseError.unauthorisedError(e.reason))
    case e                         => Left(ResponseError.internalServiceError(cause = Some(e)))
  }

  private def getEclRegistrationReference(
    authorisedFunction: AuthorisedFunction,
    request: Request[_]
  ): EitherT[Future, ResponseError, String] =
    EitherT {
      authorisedFunction
        .retrieve(Retrievals.authorisedEnrolments) { case enrolments =>
          enrolments
            .getEnrolment(EclEnrolment.serviceName)
            .flatMap(_.getIdentifier(EclEnrolment.identifierKey)) match {
            case Some(eclReference) => Future.successful(Right(eclReference.value))
            case None               => Future.successful(Left(ResponseError.internalServiceError(cause = None)))
          }
        }(hc(request), executionContext)
        .recover {
          convertToAuthorisationError[String]
        }
    }

  private def getInternalId(
    authorisedFunction: AuthorisedFunction,
    request: Request[_]
  ): EitherT[Future, ResponseError, String] =
    EitherT {
      authorisedFunction
        .retrieve(Retrievals.internalId) {
          case Some(internalId) => Future.successful(Right(internalId))
          case None             => Future.successful(Left(ResponseError.internalServiceError(cause = None)))
        }(hc(request), executionContext)
        .recover {
          convertToAuthorisationError[String]
        }
    }

  private def generateNrsIdentityData(
    internalId: String,
    authorisedFunction: AuthorisedFunction,
    request: Request[_]
  ): EitherT[Future, ResponseError, NrsIdentityData] =
    EitherT {
      authorisedFunction
        .retrieve(nrsIdentityDataRetrievals) {
          case optExternalId ~ confidenceLevel ~ optNino ~ optSaUtr ~
              optMdtpInformation ~ optCredentialStrength ~ loginTimes ~ optCredentials ~ optName ~ optDateOfBirth ~
              optEmail ~ optAffinityGroup ~ optAgentCode ~ agentInformation ~ optCredentialRole ~ optGroupIdentifier ~
              optItmpName ~ optItmpDateOfBirth ~ optItmpAddress =>
            Future.successful(
              Right(
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
            )
        }(hc(request), executionContext)
        .recover {
          convertToAuthorisationError[NrsIdentityData]
        }
    }

}
