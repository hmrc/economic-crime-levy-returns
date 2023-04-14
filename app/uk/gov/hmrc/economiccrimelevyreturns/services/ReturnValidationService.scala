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

package uk.gov.hmrc.economiccrimelevyreturns.services

import cats.data.ValidatedNel
import cats.implicits._
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError.DataMissing
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.EclReturnDetails

import javax.inject.Inject

class ReturnValidationService @Inject() () {

  type ValidationResult[A] = ValidatedNel[DataValidationError, A]

  def validateReturn(eclReturn: EclReturn): ValidationResult[EclReturnDetails] =
    (
      validateOptExists(eclReturn.relevantAp12Months, "Relevant AP 12 months choice"),
      validateConditionalOptExists(
        eclReturn.relevantApLength,
        eclReturn.relevantAp12Months.contains(false),
        "Relevant AP length"
      ),
      validateOptExists(eclReturn.relevantApRevenue, "Relevant AP revenue"),
      validateOptExists(eclReturn.calculatedLiability, "Calculated liability"),
      validateConditionalOptExists(
        eclReturn.carriedOutAmlRegulatedActivityForFullFy,
        eclReturn.calculatedLiability.isDefined && eclReturn.calculatedLiability.get.calculatedBand != Small,
        "Carried out AML regulated activity for full FY choice"
      ),
      validateConditionalOptExists(
        eclReturn.amlRegulatedActivityLength,
        eclReturn.carriedOutAmlRegulatedActivityForFullFy.contains(false),
        "AML regulated activity length"
      ),
      validateOptExists(eclReturn.contactName, "Contact name"),
      validateOptExists(eclReturn.contactRole, "Contact role"),
      validateOptExists(eclReturn.contactEmailAddress, "Contact email address"),
      validateOptExists(eclReturn.contactTelephoneNumber, "Contact telephone number")
    ).mapN((_, _, _, calculatedLiability, _, _, _, _, _, _) =>
      EclReturnDetails(amountDue = calculatedLiability.amountDue.amount)
    )

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => value.validNel
      case _           => DataValidationError(DataMissing, missingErrorMessage(description)).invalidNel
    }

  private def validateConditionalOptExists[T](
    optData: Option[T],
    condition: Boolean,
    description: String
  ): ValidationResult[Option[T]] =
    if (condition) {
      optData match {
        case Some(value) => Some(value).validNel
        case _           => DataValidationError(DataMissing, missingErrorMessage(description)).invalidNel
      }
    } else {
      None.validNel
    }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

}
