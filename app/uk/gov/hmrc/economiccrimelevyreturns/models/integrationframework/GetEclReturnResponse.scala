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

package uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.Band

final case class GetEclReturnResponse(
  chargeDetails: GetEclReturnChargeDetails,
  declarationDetails: GetEclReturnDeclarationDetails,
  eclReference: String,
  processingDateTime: String,
  returnDetails: GetEclReturnDetails,
  submissionId: Option[String],
)

object GetEclReturnResponse {
  implicit val format: OFormat[GetEclReturnResponse] = Json.format[GetEclReturnResponse]
}

final case class GetEclReturnChargeDetails(
  chargeReference: Option[String],
  periodKey: String,
  receiptDate: String,
  returnType: String
)

object GetEclReturnChargeDetails {
  implicit val format: OFormat[GetEclReturnChargeDetails] = Json.format[GetEclReturnChargeDetails]
}

final case class GetEclReturnDeclarationDetails(
  emailAddress: String,
  name: String,
  positionInCompany: String,
  telephoneNumber: String
)

object GetEclReturnDeclarationDetails {
  implicit val format: OFormat[GetEclReturnDeclarationDetails] = Json.format[GetEclReturnDeclarationDetails]
}

final case class GetEclReturnDetails(
  accountingPeriodLength: Int,
  accountingPeriodRevenue: BigDecimal,
  amountOfEclDutyLiable: BigDecimal,
  numberOfDaysRegulatedActivityTookPlace: Option[Int],
  returnDate: String,
  revenueBand: Band
)

object GetEclReturnDetails {
  implicit val format: OFormat[GetEclReturnDetails] = Json.format[GetEclReturnDetails]
}