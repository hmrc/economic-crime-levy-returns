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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataRetrievalError, DataValidationError, DataValidationErrorList, DmsSubmissionError, NrsSubmissionError, ResponseError, ReturnsSubmissionError}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.NrsSubmissionResponse
import uk.gov.hmrc.economiccrimelevyreturns.services.{AuditService, DataRetrievalService, DmsService, NrsService, ReturnService, ReturnValidationService}

import scala.concurrent.Future

class ReturnSubmissionControllerSpec extends SpecBase {

  val mockReturnValidationService: ReturnValidationService = mock[ReturnValidationService]
  val mockReturnService: ReturnService                     = mock[ReturnService]
  val mockNrsService: NrsService                           = mock[NrsService]
  val mockDmsService: DmsService                           = mock[DmsService]
  val mockAuditService: AuditService                       = mock[AuditService]
  val mockAppConfig: AppConfig                             = mock[AppConfig]
  val mockDataRetrievalService: DataRetrievalService       = mock[DataRetrievalService]

  val controller = new ReturnSubmissionController(
    cc,
    fakeAuthorisedAction,
    mockReturnValidationService,
    mockReturnService,
    mockNrsService,
    mockDmsService,
    mockAuditService,
    mockAppConfig,
    mockDataRetrievalService
  )

  override def beforeEach() = {
    reset(mockReturnValidationService)
    reset(mockReturnService)
    reset(mockNrsService)
    reset(mockDmsService)
    reset(mockAuditService)
    reset(mockDataRetrievalService)
  }

  "submitReturn" should {
    "return 200 OK with a subscription reference number in the JSON response body when the ECL return data is valid" in forAll {
      (
        eclReturn: EclReturn,
        eclReturnSubmission: EclReturnSubmission,
        returnResponse: SubmitEclReturnResponse,
        nrsSubmissionResponse: NrsSubmissionResponse
      ) =>
        beforeEach()

        val validEclReturn =
          eclReturn.copy(returnType = Some(FirstTimeReturn), base64EncodedNrsSubmissionHtml = Some("aHRtbE5ycw=="))

        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](validEclReturn))

        when(
          mockReturnService.submitEclReturn(
            anyString(),
            ArgumentMatchers.eq(eclReturnSubmission)
          )(any())
        ).thenReturn(EitherT.rightT[Future, ReturnsSubmissionError](returnResponse))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(validEclReturn)))
          .thenReturn(EitherT.rightT[Future, DataValidationErrorList](eclReturnSubmission))

        when(mockDmsService.submitToDms(anyString(), any())(any()))
          .thenReturn(EitherT.rightT[Future, DmsSubmissionError](returnResponse))

        when(mockNrsService.submitToNrs(anyString(), anyString(), anyString())(any(), any()))
          .thenReturn(EitherT.rightT[Future, NrsSubmissionError](nrsSubmissionResponse))

        val result: Future[Result] =
          controller.submitReturn(validEclReturn.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(returnResponse)

        verify(mockAuditService, times(1))
          .sendReturnSubmittedEvent(any(), any(), any())(any())
    }

    "return 200 OK when returnType is AmendReturn" in forAll {
      (
        eclReturn: EclReturn,
        eclReturnSubmission: EclReturnSubmission,
        returnResponse: SubmitEclReturnResponse,
        nrsSubmissionResponse: NrsSubmissionResponse
      ) =>
        beforeEach()

        val validEclReturn = eclReturn.copy(
          returnType = Some(AmendReturn),
          base64EncodedDmsSubmissionHtml = Some("aHRtbERNUw=="),
          base64EncodedNrsSubmissionHtml = Some("aHRtbE5ycw==")
        )

        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](validEclReturn))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(validEclReturn)))
          .thenReturn(EitherT.rightT[Future, DataValidationErrorList](eclReturnSubmission))

        when(
          mockReturnService.submitEclReturn(
            ArgumentMatchers.eq(eclRegistrationReference),
            ArgumentMatchers.eq(eclReturnSubmission)
          )(any())
        ).thenReturn(EitherT.rightT[Future, ReturnsSubmissionError](returnResponse))

        when(mockDmsService.submitToDms(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, DmsSubmissionError](returnResponse))

        when(mockNrsService.submitToNrs(anyString(), anyString(), anyString())(any(), any()))
          .thenReturn(EitherT.rightT[Future, NrsSubmissionError](nrsSubmissionResponse))

        val result: Future[Result] =
          controller.submitReturn(validEclReturn.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(returnResponse)

        verify(mockAuditService, times(1))
          .sendReturnSubmittedEvent(any(), any(), any())(any())
    }

    "return 500 BAD_REQUEST with validation errors in the JSON response body when the ECL return data is invalid" in forAll {
      (eclReturn: EclReturn, eclReturnSubmission: EclReturnSubmission, returnResponse: SubmitEclReturnResponse) =>
        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](eclReturn))

        when(
          mockReturnService.submitEclReturn(
            ArgumentMatchers.eq(eclRegistrationReference),
            ArgumentMatchers.eq(eclReturnSubmission)
          )(any())
        ).thenReturn(EitherT.rightT[Future, ReturnsSubmissionError](returnResponse))

        val validationErrorCause = "invalid returnDate"
        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(eclReturn)))
          .thenReturn(
            EitherT.leftT[Future, EclReturnSubmission](
              DataValidationErrorList(List(DataValidationError.DataMissing(validationErrorCause)))
            )
          )

        val result: Future[Result] =
          controller.submitReturn(eclReturn.internalId)(fakeRequest)

        status(result)        shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ResponseError.badRequestError(s"""
               |Data missing: $validationErrorCause
               |""".stripMargin))
    }

    "return 200 OK when returnType is AmendReturn and amendReturnsNrsEnabled is ON" in forAll {
      (
        eclReturn: EclReturn,
        eclReturnSubmission: EclReturnSubmission,
        returnResponse: SubmitEclReturnResponse,
        nrsSubmissionResponse: NrsSubmissionResponse
      ) =>
        beforeEach()

        val validEclReturn = eclReturn.copy(
          returnType = Some(AmendReturn),
          base64EncodedDmsSubmissionHtml = Some("aHRtbERNUw=="),
          base64EncodedNrsSubmissionHtml = Some("aHRtbE5ycw==")
        )

        when(mockAppConfig.amendReturnsNrsEnabled)
          .thenReturn(true)

        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](validEclReturn))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(validEclReturn)))
          .thenReturn(EitherT.rightT[Future, DataValidationErrorList](eclReturnSubmission))

        when(
          mockReturnService.submitEclReturn(
            ArgumentMatchers.eq(eclRegistrationReference),
            ArgumentMatchers.eq(eclReturnSubmission)
          )(any())
        ).thenReturn(EitherT.rightT[Future, ReturnsSubmissionError](returnResponse))

        when(mockDmsService.submitToDms(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, DmsSubmissionError](returnResponse))

        when(mockNrsService.submitToNrs(anyString(), anyString(), anyString())(any(), any()))
          .thenReturn(EitherT.rightT[Future, NrsSubmissionError](nrsSubmissionResponse))

        val result: Future[Result] =
          controller.submitReturn(validEclReturn.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(returnResponse)

        verify(mockAuditService, times(1))
          .sendReturnSubmittedEvent(any(), any(), any())(any())

        verify(mockNrsService, times(1))
          .submitToNrs(anyString(), anyString(), anyString())(any(), any())
    }

    "return 200 OK when returnType is AmendReturn and amendReturnsNrsEnabled is OFF" in forAll {
      (
        eclReturn: EclReturn,
        eclReturnSubmission: EclReturnSubmission,
        returnResponse: SubmitEclReturnResponse,
        nrsSubmissionResponse: NrsSubmissionResponse
      ) =>
        beforeEach()

        val validEclReturn = eclReturn.copy(
          returnType = Some(AmendReturn),
          base64EncodedDmsSubmissionHtml = Some("aHRtbERNUw=="),
          base64EncodedNrsSubmissionHtml = Some("aHRtbE5ycw==")
        )

        when(mockAppConfig.amendReturnsNrsEnabled)
          .thenReturn(false)

        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](validEclReturn))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(validEclReturn)))
          .thenReturn(EitherT.rightT[Future, DataValidationErrorList](eclReturnSubmission))

        when(
          mockReturnService.submitEclReturn(
            ArgumentMatchers.eq(eclRegistrationReference),
            ArgumentMatchers.eq(eclReturnSubmission)
          )(any())
        ).thenReturn(EitherT.rightT[Future, ReturnsSubmissionError](returnResponse))

        when(mockDmsService.submitToDms(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, DmsSubmissionError](returnResponse))

        when(mockNrsService.submitToNrs(anyString(), anyString(), anyString())(any(), any()))
          .thenReturn(EitherT.rightT[Future, NrsSubmissionError](nrsSubmissionResponse))

        val result: Future[Result] =
          controller.submitReturn(validEclReturn.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(returnResponse)

        verify(mockAuditService, times(1))
          .sendReturnSubmittedEvent(any(), any(), any())(any())

        verify(mockNrsService, times(0))
          .submitToNrs(anyString(), anyString(), anyString())(any(), any())
    }
  }

}
