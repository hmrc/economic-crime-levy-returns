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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait ObligationStatus

case object Open extends ObligationStatus

case object Fulfilled extends ObligationStatus

object ObligationStatus {

  implicit val format: Format[ObligationStatus] = new Format[ObligationStatus] {
    override def reads(json: JsValue): JsResult[ObligationStatus] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "O" => JsSuccess(Open)
          case "F" => JsSuccess(Fulfilled)
          case s   => JsError(s"$s is not a valid ObligationStatus")
        }
      case e: JsError          => e
    }

    override def writes(o: ObligationStatus): JsValue = o match {
      case Open      => JsString("O")
      case Fulfilled => JsString("F")
    }
  }
}
