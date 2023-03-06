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

import cats.implicits.catsSyntaxValidatedId
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataValidationError, DataValidationErrors}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError.DataInvalid
import uk.gov.hmrc.economiccrimelevyreturns.repositories.ReturnsRepository
import uk.gov.hmrc.economiccrimelevyreturns.services.ReturnValidationService

import scala.concurrent.Future

class ReturnValidationControllerSpec extends SpecBase {

  val mockReturnValidationService: ReturnValidationService = mock[ReturnValidationService]
  val mockReturnsRepository: ReturnsRepository             = mock[ReturnsRepository]

  val controller = new ReturnValidationController(
    cc,
    mockReturnsRepository,
    fakeAuthorisedAction,
    mockReturnValidationService
  )

  "getValidationErrors" should {
    "return 204 NO_CONTENT when the ECL return data is valid" in forAll { eclReturn: EclReturn =>
      when(mockReturnsRepository.get(ArgumentMatchers.eq(eclReturn.internalId)))
        .thenReturn(Future.successful(Some(eclReturn)))

      when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(eclReturn))).thenReturn(eclReturn.validNel)

      val result: Future[Result] = controller.getValidationErrors(eclReturn.internalId)(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }

    "return 200 OK with validation errors in the JSON response body when the ECL return data is invalid" in forAll {
      eclReturn: EclReturn =>
        when(mockReturnsRepository.get(ArgumentMatchers.eq(eclReturn.internalId)))
          .thenReturn(Future.successful(Some(eclReturn)))

        when(mockReturnValidationService.validateReturn(ArgumentMatchers.eq(eclReturn)))
          .thenReturn(DataValidationError(DataInvalid, "Invalid data").invalidNel)

        val result: Future[Result] = controller.getValidationErrors(eclReturn.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(
          DataValidationErrors(Seq(DataValidationError(DataInvalid, "Invalid data")))
        )
    }

    "return 404 NOT_FOUND when there is no ECL return data to validate" in forAll { eclReturn: EclReturn =>
      when(mockReturnsRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getValidationErrors(eclReturn.internalId)(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
  }

}
