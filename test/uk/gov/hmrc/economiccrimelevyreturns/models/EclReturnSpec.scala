/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.models

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._

class EclReturnSpec extends SpecBase {

  "reads" should {
    "return the return type deserialized from its JSON representation" in forAll { (returnType: ReturnType) =>
      val json = Json.toJson(returnType)

      json.as[ReturnType] shouldBe returnType
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[ReturnType](JsString("Test"))

      result shouldBe JsError("Test is not a valid ReturnType")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[ReturnType](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
