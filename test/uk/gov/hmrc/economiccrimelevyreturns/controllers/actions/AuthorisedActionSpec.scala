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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.economiccrimelevyreturns.{EnrolmentsWithEcl, EnrolmentsWithoutEcl}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError

import scala.concurrent.Future

class AuthorisedActionSpec extends SpecBase {

  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val authorisedAction =
    new BaseAuthorisedAction(mockAuthConnector, defaultBodyParser)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  private val expectedRetrievals =
    Retrievals.externalId and Retrievals.confidenceLevel and
      Retrievals.nino and Retrievals.saUtr and Retrievals.mdtpInformation and Retrievals.credentialStrength and
      Retrievals.loginTimes and Retrievals.credentials and Retrievals.dateOfBirth and Retrievals.email and
      Retrievals.affinityGroup and Retrievals.agentCode and Retrievals.agentInformation and Retrievals.credentialRole and
      Retrievals.groupIdentifier and Retrievals.itmpName and Retrievals.itmpDateOfBirth and Retrievals.itmpAddress

  "invokeBlock" should {
    "execute the block and return the result if authorised" in forAll(
      arbAuthNrsDataRetrievals(Some(alphaNumericString), enrolmentsWithEcl = true).arbitrary,
      arbEnrolmentsWithEcl.arbitrary
    ) { (authRetrievals: AuthRetrievals, enrolmentsWithEcl: EnrolmentsWithEcl) =>
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(authRetrievals))

      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(Retrievals.authorisedEnrolments))(any(), any()))
        .thenReturn(Future(enrolmentsWithEcl.enrolments))

      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(Retrievals.internalId))(any(), any()))
        .thenReturn(Future(Some(testInternalId)))

      val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Test"
    }

    "return 401 unauthorized if there is an authorisation exception" in {
      List(
        InsufficientConfidenceLevel(),
        InsufficientEnrolments(),
        UnsupportedAffinityGroup(),
        UnsupportedCredentialRole(),
        UnsupportedAuthProvider(),
        IncorrectCredentialStrength(),
        InternalError(),
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return internal server error if there is no internal id" in forAll(
      arbAuthNrsDataRetrievals(None, enrolmentsWithEcl = false).arbitrary,
      arbEnrolmentsWithEcl.arbitrary
    ) { (authRetrievals: AuthRetrievals, enrolmentsWithEcl) =>
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(Retrievals.authorisedEnrolments))(any(), any()))
        .thenReturn(Future(enrolmentsWithEcl.enrolments))

      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(Retrievals.internalId))(any(), any()))
        .thenReturn(Future(None))

      val result = await(authorisedAction.invokeBlock(fakeRequest, testAction))

      val err = ResponseError.internalServiceError(cause = None)
      result shouldBe Status(err.code.statusCode)(Json.toJson(err))
    }

    "return internal server error if there is no ECL enrolment in the set of authorised enrolments" in forAll(
      arbEnrolmentsWithoutEcl.arbitrary
    ) { (enrolmentsWithoutEcl: EnrolmentsWithoutEcl) =>
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(Retrievals.authorisedEnrolments))(any(), any()))
        .thenReturn(Future(enrolmentsWithoutEcl.enrolments))

      val result = await(authorisedAction.invokeBlock(fakeRequest, testAction))

      val err = ResponseError.internalServiceError(cause = None)
      result shouldBe Status(err.code.statusCode)(Json.toJson(err))
    }
  }

}
