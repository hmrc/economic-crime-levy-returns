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

package uk.gov.hmrc.economiccrimelevyreturns.models

import play.api.libs.json._

sealed trait Band

object Band {
  case object Small extends Band
  case object Medium extends Band
  case object Large extends Band
  case object VeryLarge extends Band

  implicit val format: Format[Band] = new Format[Band] {
    override def reads(json: JsValue): JsResult[Band] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Small"     => JsSuccess(Small)
          case "Medium"    => JsSuccess(Medium)
          case "Large"     => JsSuccess(Large)
          case "VeryLarge" => JsSuccess(VeryLarge)
          case s           => JsError(s"$s is not a valid Band")
        }
      case e: JsError          => e
    }

    override def writes(o: Band): JsValue = JsString(o.toString)
  }
}

final case class BandRange(from: Long, to: Long)

object BandRange {
  implicit val format: OFormat[BandRange] = Json.format[BandRange]
}

final case class Bands(small: BandRange, medium: BandRange, large: BandRange, veryLarge: BandRange)

object Bands {
  implicit val format: OFormat[Bands] = Json.format[Bands]
}

final case class CalculatedLiability(amountDue: BigDecimal, bands: Bands, calculatedBand: Band)

object CalculatedLiability {
  implicit val format: OFormat[CalculatedLiability] = Json.format[CalculatedLiability]
}
