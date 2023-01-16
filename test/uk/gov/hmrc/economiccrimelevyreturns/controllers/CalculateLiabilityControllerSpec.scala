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

import org.mockito.ArgumentMatchers.any
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculateLiabilityRequest, CalculatedLiability}
import uk.gov.hmrc.economiccrimelevyreturns.services.CalculateLiabilityService

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class CalculateLiabilityControllerSpec extends SpecBase {

  val mockCalculateLiabilityService: CalculateLiabilityService = mock[CalculateLiabilityService]

  val controller = new CalculateLiabilityController(
    cc,
    fakeAuthorisedAction,
    mockCalculateLiabilityService
  )

  val minAmountDue = 0
  val maxAmountDue = 250000

  implicit val arbAmountDue: Arbitrary[BigDecimal] = Arbitrary {
    Gen.chooseNum[Double](minAmountDue, maxAmountDue).map(BigDecimal.apply(_).setScale(2, RoundingMode.DOWN))
  }

  "calculateLiability" should {
    "return 200 OK the calculated liability JSON" in forAll {
      (calculateLiabilityRequest: CalculateLiabilityRequest, calculatedLiability: CalculatedLiability) =>
        when(mockCalculateLiabilityService.calculateLiability(any())).thenReturn(calculatedLiability)

        val result: Future[Result] =
          controller.calculateLiability()(
            fakeRequestWithJsonBody(Json.toJson(calculateLiabilityRequest))
          )

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(calculatedLiability)
    }
  }

}
