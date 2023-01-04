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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}

import java.util.Date

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

final case class ObligationData(
  obligations: Seq[Obligation]
)

object ObligationData {
  implicit val format: OFormat[ObligationData] = Json.format[ObligationData]
}

final case class Obligation(
  identification: Option[Identification],
  obligationDetails: Seq[ObligationDetails]
)

object Obligation {
  implicit val format: OFormat[Obligation] = Json.format[Obligation]
}

final case class Identification(
  incomeSourceType: Option[String],
  referenceNumber: String,
  referenceType: String
)

object Identification {
  implicit val format: OFormat[Identification] = Json.format[Identification]
}

final case class ObligationDetails(
  status: ObligationStatus,
  inboundCorrespondenceFromDate: Date,
  inboundCorrespondenceToDate: Date,
  inboundCorrespondenceDateReceived: Option[Date],
  inboundCorrespondenceDueDate: Date,
  periodKey: String
)

object ObligationDetails {
  implicit val format: OFormat[ObligationDetails] = Json.format[ObligationDetails]
}
