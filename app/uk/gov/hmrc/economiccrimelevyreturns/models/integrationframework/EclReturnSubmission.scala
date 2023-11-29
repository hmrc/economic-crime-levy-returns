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

package uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.Band

final case class EclReturnDetails(
  revenueBand: Band,
  amountOfEclDutyLiable: Double,
  accountingPeriodRevenue: Double,
  accountingPeriodLength: Int,
  numberOfDaysRegulatedActivityTookPlace: Option[Int],
  returnDate: String
)

object EclReturnDetails {
  implicit val format: OFormat[EclReturnDetails] = Json.format[EclReturnDetails]
}

final case class DeclarationDetails(
  name: String,
  positionInCompany: String,
  emailAddress: String,
  telephoneNumber: String
)

object DeclarationDetails {
  implicit val format: OFormat[DeclarationDetails] = Json.format[DeclarationDetails]
}

final case class EclReturnSubmission(
  periodKey: String,
  returnDetails: EclReturnDetails,
  declarationDetails: DeclarationDetails
)

object EclReturnSubmission {
  implicit val format: OFormat[EclReturnSubmission] = Json.format[EclReturnSubmission]
}
