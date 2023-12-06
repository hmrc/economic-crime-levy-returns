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
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ReturnsSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class IntegrationFrameworkServiceSpec extends SpecBase {

  private val mockIntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]

  val service = new IntegrationFrameworkService(mockIntegrationFrameworkConnector)

  "submitEclReturn" should {
    "return successful submit return response when call to the integration framework succeeds" in forAll {
      (
        eclReturnSubmission: EclReturnSubmission,
        returnResponse: SubmitEclReturnResponse
      ) =>
        when(
          mockIntegrationFrameworkConnector
            .submitEclReturn(ArgumentMatchers.eq(eclRegistrationReference), ArgumentMatchers.eq(eclReturnSubmission))(
              any()
            )
        )
          .thenReturn(Future.successful(returnResponse))

        val result = await(service.submitEclReturn(eclRegistrationReference, eclReturnSubmission).value)

        result shouldBe Right(returnResponse)
    }

    "return ReturnsSubmissionError.BadGateway when call to the integration framework returns an error response" in forAll(
      generateErrorCode,
      arbValidEclReturn.arbitrary
    ) {
      (
        errorCode: Int,
        eclReturn: ValidEclReturn
      ) =>
        val message = "Gateway Error"

        when(
          mockIntegrationFrameworkConnector
            .submitEclReturn(
              ArgumentMatchers.eq(eclRegistrationReference),
              ArgumentMatchers.eq(eclReturn.expectedEclReturnSubmission)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

        val result =
          await(service.submitEclReturn(eclRegistrationReference, eclReturn.expectedEclReturnSubmission).value)

        result shouldBe Left(ReturnsSubmissionError.BadGateway(message, errorCode))
    }

    "return ReturnsSubmissionError.InternalUnexpectedError when an exception while submitting return in the integration framework connector" in forAll {
      (
        eclReturn: ValidEclReturn
      ) =>
        val exception = new Exception("Unexpected error")

        when(
          mockIntegrationFrameworkConnector
            .submitEclReturn(
              ArgumentMatchers.eq(eclRegistrationReference),
              ArgumentMatchers.eq(eclReturn.expectedEclReturnSubmission)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(exception))

        val result =
          await(service.submitEclReturn(eclRegistrationReference, eclReturn.expectedEclReturnSubmission).value)

        result shouldBe Left(ReturnsSubmissionError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }
  }
}
