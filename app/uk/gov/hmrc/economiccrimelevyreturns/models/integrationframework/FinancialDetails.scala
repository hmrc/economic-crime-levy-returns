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

import java.util.Date

final case class FinancialDetail(
  taxYear: String,
  chargeType: Option[String],
  chargeReference: Option[String],
  periodKey: Option[String],
  originalAmount: Option[BigDecimal],
  outstandingAmount: Option[BigDecimal],
  clearedAmount: Option[BigDecimal],
  accruedInterest: Option[BigDecimal],
  items: Seq[FinancialItem]
)

object FinancialDetail {
  implicit val format: OFormat[FinancialDetail] = Json.format[FinancialDetail]
}

final case class FinancialItem(
  dueDate: Option[Date],
  amount: Option[BigDecimal],
  clearingDate: Option[Date],
  paymentAmount: Option[BigDecimal]
)

object FinancialItem {
  implicit val format: OFormat[FinancialItem] = Json.format[FinancialItem]
}

final case class FinancialDetails(financialDetails: Option[Seq[FinancialDetail]])

object FinancialDetails {
  implicit val format: OFormat[FinancialDetails] = Json.format[FinancialDetails]
}
