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

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

final case class EclReturn(
  internalId: String,
  relevantAp12Months: Option[Boolean],
  relevantApLength: Option[Int],
  relevantApRevenue: Option[Long],
  carriedOutAmlRegulatedActivityForFullFy: Option[Boolean],
  amlRegulatedActivityLength: Option[Int],
  calculatedLiability: Option[CalculatedLiability],
  contactName: Option[String],
  contactRole: Option[String],
  contactEmailAddress: Option[String],
  contactTelephoneNumber: Option[String],
  obligationDetails: Option[ObligationDetails],
  lastUpdated: Option[Instant] = None
)

object EclReturn {
  def empty(internalId: String): EclReturn = EclReturn(
    internalId = internalId,
    relevantAp12Months = None,
    relevantApLength = None,
    relevantApRevenue = None,
    carriedOutAmlRegulatedActivityForFullFy = None,
    amlRegulatedActivityLength = None,
    calculatedLiability = None,
    contactName = None,
    contactRole = None,
    contactEmailAddress = None,
    contactTelephoneNumber = None,
    obligationDetails = None
  )

  implicit val format: OFormat[EclReturn] = Json.format[EclReturn]
}
