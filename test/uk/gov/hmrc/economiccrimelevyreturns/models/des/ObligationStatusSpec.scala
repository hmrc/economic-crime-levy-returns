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

package uk.gov.hmrc.economiccrimelevyreturns.models.des

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class ObligationStatusSpec extends SpecBase {
  "writes" should {
    "return the obligation status serialized to its JSON representation" in forAll(
      Table(("obligationStatus", "expectedResult"), (Open, "O"), (Fulfilled, "F"))
    ) { (obligationStatus: ObligationStatus, expectedResult: String) =>
      val result = Json.toJson(obligationStatus)

      result shouldBe JsString(expectedResult)
    }
  }

  "reads" should {
    "return the obligation status deserialized from its JSON representation" in forAll {
      (obligationStatus: ObligationStatus) =>
        val json = Json.toJson(obligationStatus)

        json.as[ObligationStatus] shouldBe obligationStatus
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[ObligationStatus](JsString("Test"))

      result shouldBe JsError("Test is not a valid ObligationStatus")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[ObligationStatus](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
