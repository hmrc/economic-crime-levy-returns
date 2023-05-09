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
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.NrsSubmissionResponse

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  val mockNrsConnector: NrsConnector       = mock[NrsConnector]
  private val fixedPointInTime             = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock             = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)
  private val fakeRequestWithAuthorisation = fakeRequest.withHeaders((HeaderNames.AUTHORIZATION, "test"))

  val service = new NrsService(mockNrsConnector, stubClock)

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
          service.submitToNrs(
            Some(validNrsSubmission.base64EncodedNrsSubmissionHtml),
            validNrsSubmission.eclRegistrationReference
          )(hc, request)
        )

      result shouldBe nrsSubmissionResponse
    }

    "throw an IllegalStateException when there is no base64 encoded NRS submission HTML" in forAll(
      arbValidNrsSubmission(fakeRequestWithAuthorisation, stubClock).arbitrary
    ) { validNrsSubmission: ValidNrsSubmission =>
      val request = AuthorisedRequest(
        fakeRequestWithAuthorisation,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.eclRegistrationReference,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val result = intercept[IllegalStateException] {
        await(
          service.submitToNrs(
            None,
            validNrsSubmission.eclRegistrationReference
          )(hc, request)
        )
      }

      result.getMessage shouldBe "Base64 encoded NRS submission HTML not found in registration data"
    }
  }

}
