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
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.connectors.{HipReturnsConnector, IntegrationFrameworkReturnConnector}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries.*
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ReturnsSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.{ValidEclReturn, ValidGetEclReturnSubmissionResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class ReturnSubmissionServiceSpec extends SpecBase {

  private val mockIntegrationFrameworkReturnConnector = mock[IntegrationFrameworkReturnConnector]
  private val mockHipReturnConnector                  = mock[HipReturnsConnector]

  private def serviceWith(hipEnabled: Boolean): ReturnSubmissionService = {
    val config = mock[AppConfig]
    when(config.hipReturns).thenReturn(hipEnabled)
    new ReturnSubmissionService(config, mockIntegrationFrameworkReturnConnector, mockHipReturnConnector)
  }

  val ifService    = serviceWith(hipEnabled = false)
  val hipService = serviceWith(hipEnabled = true)

  "getEclReturnSubmission" should {
    "return successful return submission response when call to the integration framework succeeds" in forAll {
      (
        validResponse: ValidGetEclReturnSubmissionResponse
      ) =>
        when(
          mockIntegrationFrameworkReturnConnector
            .getEclReturnSubmission(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(testEclRegistrationReference))(
              any()
            )
        )
          .thenReturn(Future.successful(validResponse.response))

        val result = await(ifService.getEclReturnSubmission(periodKey, testEclRegistrationReference).value)

        result shouldBe Right(validResponse.response)
    }

    "return ReturnsSubmissionError.BadGateway when call to the integration framework returns an error response" in forAll(
      generateErrorCode
    ) {
      (
        errorCode: Int
      ) =>
        val message       = "Gateway Error"
        val errorMessages = s"Get ECL Return Failed - $message"

        when(
          mockIntegrationFrameworkReturnConnector
            .getEclReturnSubmission(
              ArgumentMatchers.eq(periodKey),
              ArgumentMatchers.eq(testEclRegistrationReference)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

        val result =
          await(ifService.getEclReturnSubmission(periodKey, testEclRegistrationReference).value)

        result shouldBe Left(ReturnsSubmissionError.BadGateway(errorMessages, errorCode))
    }

    "return ReturnsSubmissionError.InternalUnexpectedError when an exception while submitting return in the integration framework connector" in {
      val exception = new Exception("Unexpected error")

      when(
        mockIntegrationFrameworkReturnConnector
          .getEclReturnSubmission(
            ArgumentMatchers.eq(periodKey),
            ArgumentMatchers.eq(testEclRegistrationReference)
          )(
            any()
          )
      )
        .thenReturn(Future.failed(exception))

      val result =
        await(ifService.getEclReturnSubmission(periodKey, testEclRegistrationReference).value)

      result shouldBe Left(ReturnsSubmissionError.InternalUnexpectedError(Some(exception)))
    }
  }

  "submitEclReturn" should {
    "return successful submit return response when call to the integration framework succeeds" in forAll {
      (
        eclReturnSubmission: EclReturnSubmission,
        returnResponse: SubmitEclReturnResponse
      ) =>
        when(
          mockIntegrationFrameworkReturnConnector
            .submitEclReturn(
              ArgumentMatchers.eq(testEclRegistrationReference),
              ArgumentMatchers.eq(eclReturnSubmission)
            )(
              any()
            )
        )
          .thenReturn(Future.successful(returnResponse))

        val result = await(ifService.submitEclReturn(testEclRegistrationReference, eclReturnSubmission).value)

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
        val message      = "Gateway Error"
        val errorMessage = s"Submit ECL Return Failed - $message"

        when(
          mockIntegrationFrameworkReturnConnector
            .submitEclReturn(
              ArgumentMatchers.eq(testEclRegistrationReference),
              ArgumentMatchers.eq(eclReturn.expectedEclReturnSubmission)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

        val result =
          await(ifService.submitEclReturn(testEclRegistrationReference, eclReturn.expectedEclReturnSubmission).value)

        result shouldBe Left(ReturnsSubmissionError.BadGateway(errorMessage, errorCode))
    }

    "return ReturnsSubmissionError.InternalUnexpectedError when an exception while submitting return in the integration framework connector" in forAll {
      (
        eclReturn: ValidEclReturn
      ) =>
        val exception = new Exception("Unexpected error")

        when(
          mockIntegrationFrameworkReturnConnector
            .submitEclReturn(
              ArgumentMatchers.eq(testEclRegistrationReference),
              ArgumentMatchers.eq(eclReturn.expectedEclReturnSubmission)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(exception))

        val result =
          await(ifService.submitEclReturn(testEclRegistrationReference, eclReturn.expectedEclReturnSubmission).value)

        result shouldBe Left(ReturnsSubmissionError.InternalUnexpectedError(Some(exception)))
    }
  }

  "connector selection" should {
    "route getEclReturnSubmission to the integration framework when the hip returns feature is disabled" in {
      reset(mockIntegrationFrameworkReturnConnector)
      reset(mockHipReturnConnector)

      val expected = random[ValidGetEclReturnSubmissionResponse].response

      when(mockIntegrationFrameworkReturnConnector.getEclReturnSubmission(any(), any())(any()))
        .thenReturn(Future.successful(expected))

      val result = await(ifService.getEclReturnSubmission(periodKey, testEclRegistrationReference).value)

      result shouldBe Right(expected)
      verify(mockIntegrationFrameworkReturnConnector, times(1)).getEclReturnSubmission(any(), any())(any())
      verify(mockHipReturnConnector, times(0)).getEclReturnSubmission(any(), any())(any())
    }

    "route getEclReturnSubmission to hip when the hip returns feature is enabled" in {
      reset(mockIntegrationFrameworkReturnConnector)
      reset(mockHipReturnConnector)

      val expected = random[ValidGetEclReturnSubmissionResponse].response

      when(mockHipReturnConnector.getEclReturnSubmission(any(), any())(any()))
        .thenReturn(Future.successful(expected))

      val result = await(hipService.getEclReturnSubmission(periodKey, testEclRegistrationReference).value)

      result shouldBe Right(expected)
      verify(mockHipReturnConnector, times(1)).getEclReturnSubmission(any(), any())(any())
      verify(mockIntegrationFrameworkReturnConnector, times(0)).getEclReturnSubmission(any(), any())(any())
    }

    "route submitEclReturn to the integration framework when the hip returns feature is disabled" in {
      reset(mockIntegrationFrameworkReturnConnector)
      reset(mockHipReturnConnector)

      val submission = random[EclReturnSubmission]
      val expected   = random[SubmitEclReturnResponse]

      when(mockIntegrationFrameworkReturnConnector.submitEclReturn(any(), any())(any()))
        .thenReturn(Future.successful(expected))

      val result = await(ifService.submitEclReturn(testEclRegistrationReference, submission).value)

      result shouldBe Right(expected)
      verify(mockIntegrationFrameworkReturnConnector, times(1)).submitEclReturn(any(), any())(any())
      verify(mockHipReturnConnector, times(0)).submitEclReturn(any(), any())(any())
    }

    "route submitEclReturn to hip when the hip returns feature is enabled" in {
      reset(mockIntegrationFrameworkReturnConnector)
      reset(mockHipReturnConnector)

      val submission = random[EclReturnSubmission]
      val expected   = random[SubmitEclReturnResponse]

      when(mockHipReturnConnector.submitEclReturn(any(), any())(any()))
        .thenReturn(Future.successful(expected))

      val result = await(hipService.submitEclReturn(testEclRegistrationReference, submission).value)

      result shouldBe Right(expected)
      verify(mockHipReturnConnector, times(1)).submitEclReturn(any(), any())(any())
      verify(mockIntegrationFrameworkReturnConnector, times(0)).submitEclReturn(any(), any())(any())
    }
  }
}
