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

import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.economiccrimelevyreturns.models.hip.HipPlatformErrors.*

class HipPlatformErrorsSpec extends AnyWordSpec with Matchers {

  val failuresArrayErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "Type of Failure",
      |        "reason": "Reason for Failure"
      |      },
      |      {
      |        "type": "Another type of Failure",
      |        "reason": "More reasons"
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin)

  val objErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HoD",
      |  "response": {
      |    "error": {
      |      "code": "400",
      |      "logID": "00000000000000000000000000000000",
      |      "message": "String"
      |    }
      |  }
      |}
      |""".stripMargin)

  val unexpectedErr: JsValue =
    Json.parse("""
      |{
      |  "origin": "HIP",
      |  "response": {
      |    "unexpectedError": {
      |      "status": 503,
      |      "body": "down"
      |    }
      |  }
      |}
      |""".stripMargin)

  val err422: JsValue =
    Json.parse("""
      |{
      |  "error": {
      |    "errorId": "044",
      |    "processingDate": "2022-01-31T09:26:17Z",
      |    "text": "Tax Obligation Already Fulfilled"
      |  }
      |}
      |""".stripMargin)

  "HipPlatformErrors" should {
    "be read and written correctly" in
      forAll(Seq(failuresArrayErr, objErr, unexpectedErr)) { jsValue =>
        Json.toJson(jsValue.as[HipErrorWrapper]) shouldBe jsValue
      }

    "read the HoD origin with a system error" in {
      val wrapper = objErr.as[HipErrorWrapper]

      wrapper.origin   shouldBe Origin.HoD
      wrapper.response shouldBe HipSystemErrorObject(HipError("400", "00000000000000000000000000000000", "String"))
    }

    "read the HIP origin with failures" in {
      val wrapper = failuresArrayErr.as[HipErrorWrapper]

      wrapper.origin shouldBe Origin.HIP
      wrapper.response match {
        case HipFailuresErrorArray(failures) =>
          failures.toSeq shouldBe Seq(
            HipFailure("Type of Failure", "Reason for Failure"),
            HipFailure("Another type of Failure", "More reasons")
          )
        case other                           => fail(s"expected failures, got $other")
      }
    }

    "reject an unknown origin" in {
      JsString("ETMP").validate[Origin].isError                 shouldBe true
      Json.obj("origin" -> 1).validate[HipErrorWrapper].isError shouldBe true
    }

    "reject a response that is neither failures nor an error" in {
      Json.obj("origin" -> "HIP", "response" -> Json.obj("other" -> 1)).validate[HipErrorWrapper].isError shouldBe true
    }
  }

  "Hip422Error" should {
    "be read and written correctly" in {
      val parsed = err422.as[Hip422Error]

      parsed              shouldBe Hip422Error(HipInner422Err("044", "2022-01-31T09:26:17Z", "Tax Obligation Already Fulfilled"))
      Json.toJson(parsed) shouldBe err422
    }
  }
}
