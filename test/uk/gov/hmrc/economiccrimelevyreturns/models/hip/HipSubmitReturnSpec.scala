/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.models.hip

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries.*
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse

import java.time.Instant

class HipSubmitReturnSpec extends SpecBase {

  private val jsonPayload = Json.parse(
    """
      |{
      |  "success": {
      |    "processingDate": "2022-04-01T19:43:17Z",
      |    "eclReference": "XMECL1114808092",
      |    "submissionId": "789124231021",
      |    "chargeReference": "XY007000075424",
      |    "amount": 10000,
      |    "dueDate": "2022-04-01"
      |  }
      |}
    """.stripMargin
  )

  "HipSubmitReturnSuccessResponse format" should {
    "deserialise the documented hip success payload" in {
      val result = jsonPayload.validate[HipSubmitReturnSuccessResponse]

      result shouldBe JsSuccess(
        HipSubmitReturnSuccessResponse(
          HipSubmitReturn(
            processingDate = Instant.parse("2022-04-01T19:43:17Z"),
            eclReference = "XMECL1114808092",
            submissionId = Some("789124231021"),
            chargeReference = Some("XY007000075424"),
            amount = BigDecimal(10000),
            dueDate = "2022-04-01"
          )
        )
      )
    }

    "deserialise a payload with the optional fields absent" in {
      val json = Json.obj(
        "success" -> Json.obj(
          "processingDate" -> "2022-04-01T19:43:17Z",
          "eclReference"   -> "XMECL1114808092",
          "amount"         -> 10000,
          "dueDate"        -> "2022-04-01"
        )
      )

      val result = json.validate[HipSubmitReturnSuccessResponse].asOpt.map(_.success)

      result.flatMap(_.submissionId)    shouldBe None
      result.flatMap(_.chargeReference) shouldBe None
    }

    "fail to deserialise when a required field is missing" in {
      val json = Json.obj(
        "success" -> Json.obj(
          "processingDate" -> "2022-04-01T19:43:17Z",
          "submissionId"   -> "789124231021",
          "amount"         -> 10000,
          "dueDate"        -> "2022-04-01"
        )
      )

      json.validate[HipSubmitReturnSuccessResponse].isError shouldBe true
    }

    "fail to deserialise a payload that is not wrapped in a success envelope" in {
      val unwrapped = (jsonPayload \ "success").get

      unwrapped.validate[HipSubmitReturnSuccessResponse].isError shouldBe true
    }

    "round trip any generated hip submit return" in forAll { (hipSubmitReturn: HipSubmitReturn) =>
      val wrapper = HipSubmitReturnSuccessResponse(hipSubmitReturn)

      Json.toJson(wrapper).validate[HipSubmitReturnSuccessResponse] shouldBe JsSuccess(wrapper)
    }
  }

  "toIfResponse" should {
    "keep only the fields the ecl returns api exposes" in {
      val hipSubmitReturn = jsonPayload.as[HipSubmitReturnSuccessResponse].success

      hipSubmitReturn.toIfResponse shouldBe SubmitEclReturnResponse(
        processingDate = Instant.parse("2022-04-01T19:43:17Z"),
        chargeReference = Some("XY007000075424")
      )
    }

    "carry processingDate and chargeReference across unchanged for any generated response" in forAll {
      (hipSubmitReturn: HipSubmitReturn) =>
        val result = hipSubmitReturn.toIfResponse

        result.processingDate  shouldBe hipSubmitReturn.processingDate
        result.chargeReference shouldBe hipSubmitReturn.chargeReference
    }
  }

  "HipSuccessWrapper" should {
    "unwrap the success envelope" in forAll { (hipSubmitReturn: HipSubmitReturn) =>
      val json = Json.obj("success" -> Json.toJson(hipSubmitReturn))

      // asOpt rather than JsSuccess, because reading through `__ \ "success"` carries that path in the result
      json.validate[HipSuccessWrapper[HipSubmitReturn]].asOpt shouldBe Some(HipSuccessWrapper(hipSubmitReturn))
    }

    "fail when the success envelope is absent" in forAll { (hipSubmitReturn: HipSubmitReturn) =>
      Json.toJson(hipSubmitReturn).validate[HipSuccessWrapper[HipSubmitReturn]].isError shouldBe true
    }
  }
}
