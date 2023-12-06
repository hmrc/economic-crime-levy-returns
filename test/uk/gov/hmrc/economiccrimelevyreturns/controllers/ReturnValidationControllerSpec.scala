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
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataRetrievalError, DataValidationError, DataValidationErrorList, ResponseError}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.EclReturnSubmission
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnsService, ReturnValidationService}

import scala.concurrent.Future

class ReturnValidationControllerSpec extends SpecBase {

  val mockReturnValidationService: ReturnValidationService = mock[ReturnValidationService]
  val mockDataRetrievalService: ReturnsService       = mock[ReturnsService]

  val controller = new ReturnValidationController(
    cc,
    mockDataRetrievalService,
    fakeAuthorisedAction,
    mockReturnValidationService
  )

  "getValidationErrors" should {
    "return 200 OK when the ECL return data is valid" in forAll {
      (eclReturn: EclReturn, eclReturnSubmission: EclReturnSubmission) =>
        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](eclReturn))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(eclReturn)))
          .thenReturn(EitherT.right[DataValidationErrorList](Future.successful(eclReturnSubmission)))

        val result: Future[Result] = controller.getValidationErrors(eclReturn.internalId)(fakeRequest)

        status(result) shouldBe OK
    }

    "return 400 BAD_REQUEST with validation errors in the JSON response body when the ECL return data is invalid" in forAll {
      eclReturn: EclReturn =>
        when(mockDataRetrievalService.get(anyString())(any()))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](eclReturn))

        val errorMessage     = "Invalid data"
        val validationErrors = DataValidationErrorList(List(DataValidationError.DataMissing(errorMessage)))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(eclReturn)))
          .thenReturn(EitherT.left[EclReturnSubmission](Future.successful(validationErrors)))

        val result: Future[Result] = controller.getValidationErrors(eclReturn.internalId)(fakeRequest)

        status(result)        shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ResponseError.badRequestError(s"""
             |Data missing: $errorMessage
             |""".stripMargin))
    }

    "return 404 NOT_FOUND when there is no ECL return data to validate" in forAll { eclReturn: EclReturn =>
      when(mockDataRetrievalService.get(anyString())(any()))
        .thenReturn(EitherT.leftT[Future, EclReturn](DataRetrievalError.NotFound(eclReturn.internalId)))

      val result: Future[Result] = controller.getValidationErrors(eclReturn.internalId)(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${eclReturn.internalId}")
      )
    }
  }

}
