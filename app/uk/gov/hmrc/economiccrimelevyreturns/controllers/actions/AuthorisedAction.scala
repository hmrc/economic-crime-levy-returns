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

import com.google.inject.Inject
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{authorisedEnrolments, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

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

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    authorised(Enrolment(EclEnrolment.ServiceName)).retrieve(internalId and authorisedEnrolments) {
      case optInternalId ~ enrolments =>
        val internalId               = optInternalId.getOrElseFail("Unable to retrieve internalId")
        val eclRegistrationReference =
          enrolments
            .getEnrolment(EclEnrolment.ServiceName)
            .flatMap(_.getIdentifier(EclEnrolment.IdentifierKey))
            .getOrElseFail(
              s"Unable to retrieve enrolment with key ${EclEnrolment.ServiceName} and identifier ${EclEnrolment.IdentifierKey}"
            )
            .value
        block(AuthorisedRequest(request, internalId, eclRegistrationReference))
    }(hc(request), executionContext) recover { case e: AuthorisationException =>
      Unauthorized(
        Json.toJson(
          ErrorResponse(
            UNAUTHORIZED,
            e.reason
          )
        )
      )
    }

  implicit class OptionOps[T](o: Option[T]) {
    def getOrElseFail(failureMessage: String): T = o.getOrElse(throw new IllegalStateException(failureMessage))
  }

}
