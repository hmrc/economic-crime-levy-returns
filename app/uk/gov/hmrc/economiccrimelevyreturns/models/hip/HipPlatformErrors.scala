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

import play.api.libs.json.*

object HipPlatformErrors {

  enum Origin:
    case HoD, HIP

  given Format[Origin] = Format(
    Reads:
      case JsString("HoD") => JsSuccess(Origin.HoD)
      case JsString("HIP") => JsSuccess(Origin.HIP)
      case JsString(_)     => JsError("Invalid origin string")
      case _               => JsError("Expected JsString")
    ,
    Writes:
      case Origin.HoD => JsString("HoD")
      case Origin.HIP => JsString("HIP")
  )

  case class HipFailure(`type`: String, reason: String)
  case class HipError(code: String, logID: String, message: String)

  sealed trait HipErrorTrait
  case class HipSystemErrorObject(error: HipError) extends HipErrorTrait
  case class HipFailuresErrorArray(failures: Array[HipFailure]) extends HipErrorTrait
  case class HipUnexpectedError(status: Int, body: String) extends HipErrorTrait

  given hipErrorTraitFormat: Format[HipErrorTrait] = Format(
    Reads:
      case fails: JsObject if (fails \ "failures").isDefined    =>
        val failures: Array[HipFailure] = (fails \ "failures")
          .as[JsArray]
          .value
          .map { x =>
            val t = (x \ "type").as[String]
            val r = (x \ "reason").as[String]
            HipFailure(t, r)
          }
          .toArray
        JsSuccess(HipFailuresErrorArray(failures))
      case obj: JsObject if (obj \ "error").isDefined           =>
        JsSuccess(
          HipSystemErrorObject(
            HipError(
              (obj \ "error" \ "code").as[String],
              (obj \ "error" \ "logID").as[String],
              (obj \ "error" \ "message").as[String]
            )
          )
        )
      case obj: JsObject if (obj \ "unexpectedError").isDefined =>
        JsSuccess(
          HipUnexpectedError(
            (obj \ "unexpectedError" \ "status").as[Int],
            (obj \ "unexpectedError" \ "body").as[String]
          )
        )
      case badJson                                              => JsError(s"Expected HipErrorTrait Json... got $badJson")
    ,
    Writes:
      case HipSystemErrorObject(HipError(code, logID, message)) =>
        Json.obj("error" -> Json.obj("code" -> code, "logID" -> logID, "message" -> message))
      case HipFailuresErrorArray(failures)                      =>
        Json.obj("failures" -> failures.map(x => Json.obj("type" -> x.`type`, "reason" -> x.reason)).toSeq)
      case HipUnexpectedError(status, body)                     =>
        Json.obj("unexpectedError" -> Json.obj("status" -> status, "body" -> body))
  )

  case class HipErrorWrapper(origin: Origin, response: HipErrorTrait)

  given failureFormat: OFormat[HipFailure]                          = Json.format[HipFailure]
  given errorFormat: OFormat[HipError]                              = Json.format[HipError]
  given hipSystemErrorObjectFormat: OFormat[HipSystemErrorObject]   = Json.format[HipSystemErrorObject]
  given hipFailuresErrorArrayFormat: OFormat[HipFailuresErrorArray] = Json.format[HipFailuresErrorArray]
  given unexpectedErrorFormat: OFormat[HipUnexpectedError]          = Json.format[HipUnexpectedError]
  given hipErrorWrapperFormat: OFormat[HipErrorWrapper]             = Json.format[HipErrorWrapper]
}
