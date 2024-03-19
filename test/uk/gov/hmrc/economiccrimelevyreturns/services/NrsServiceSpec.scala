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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.http.HeaderNames
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.ValidNrsSubmission
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.NrsSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.NrsSubmissionResponse
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  val mockNrsConnector: NrsConnector       = mock[NrsConnector]
  private val fixedPointInTime             = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock             = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)
  private val fakeRequestWithAuthorisation = fakeRequest.withHeaders((HeaderNames.AUTHORIZATION, "test"))

  val service = new NrsService(mockNrsConnector, stubClock)

  val returnNotableEvent      = "ecl-return"
  val amendReturnNotableEvent = "ecl-amend-return"

  override def beforeEach(): Unit =
    reset(mockNrsConnector)

  "submitToNrs" should {
    "return the NRS submission ID when the submission is successful" in forAll(
      arbValidNrsSubmission(fakeRequestWithAuthorisation, stubClock).arbitrary,
      Arbitrary.arbitrary[NrsSubmissionResponse]
    ) { (validNrsSubmission: ValidNrsSubmission, nrsSubmissionResponse: NrsSubmissionResponse) =>
      when(mockNrsConnector.submitToNrs(ArgumentMatchers.eq(validNrsSubmission.nrsSubmission))(any()))
        .thenReturn(Future.successful(nrsSubmissionResponse))

      val request = AuthorisedRequest(
        fakeRequestWithAuthorisation,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.eclRegistrationReference,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val result =
        await(
          service
            .submitToNrs(
              validNrsSubmission.base64EncodedNrsSubmissionHtml,
              validNrsSubmission.eclRegistrationReference,
              returnNotableEvent
            )(hc, request)
            .value
        )

      result shouldBe Right(nrsSubmissionResponse)
    }

    "return NrsSubmissionError.InternalUnexpectedError when the authorization header is empty" in forAll(
      arbValidNrsSubmission(fakeRequest, stubClock).arbitrary
    ) { (validNrsSubmission: ValidNrsSubmission) =>
      val request = AuthorisedRequest(
        fakeRequest,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.eclRegistrationReference,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val result =
        await(
          service
            .submitToNrs(
              "YQ==",
              validNrsSubmission.eclRegistrationReference,
              returnNotableEvent
            )(hc, request)
            .value
        )

      result shouldBe Left(NrsSubmissionError.InternalUnexpectedError(None))
    }

    "return NrsSubmissionError.InternalUnexpectedError when an exception is thrown while decoding due to invalid base 64 string" in forAll(
      arbValidNrsSubmission(fakeRequestWithAuthorisation, stubClock).arbitrary
    ) { validNrsSubmission: ValidNrsSubmission =>
      val request = AuthorisedRequest(
        fakeRequestWithAuthorisation,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.eclRegistrationReference,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val result =
        await(
          service
            .submitToNrs(
              "_",
              validNrsSubmission.eclRegistrationReference,
              returnNotableEvent
            )(hc, request)
            .value
        )

      result match {
        case Left(e)  => e.isInstanceOf[NrsSubmissionError] shouldBe true
        case Right(_) => fail("Expected exception to be thrown for invalid base64 value")
      }
    }

    "return NrsSubmissionError.BadGateway when call to NRS returns an error response" in forAll(
      arbValidNrsSubmission(fakeRequestWithAuthorisation, stubClock).arbitrary
    ) { (validNrsSubmission: ValidNrsSubmission) =>
      val request = AuthorisedRequest(
        fakeRequestWithAuthorisation,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.eclRegistrationReference,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val errorCode            = NOT_ACCEPTABLE
      val upstreamErrorMessage = "Gateway Error"
      val errorMessage         = s"NRS Submission Failed - $upstreamErrorMessage"

      when(mockNrsConnector.submitToNrs(ArgumentMatchers.eq(validNrsSubmission.nrsSubmission))(any[HeaderCarrier]()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply(upstreamErrorMessage, errorCode)))

      val result =
        await(
          service
            .submitToNrs(
              validNrsSubmission.base64EncodedNrsSubmissionHtml,
              validNrsSubmission.eclRegistrationReference,
              returnNotableEvent
            )(hc, request)
            .value
        )

      result shouldBe Left(NrsSubmissionError.BadGateway(errorMessage, errorCode))
    }
  }

}
