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

import cats.data.Validated.{Invalid, Valid}
import cats.data.{EitherT, ValidatedNel}
import cats.implicits._
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyreturns.utils.{SchemaLoader, SchemaValidator}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrorList

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.Future

class ReturnValidationService @Inject() (clock: Clock, schemaValidator: SchemaValidator) {

  type ValidationResult[A] = ValidatedNel[DataValidationError, A]

  private val YearInDays: Int = 365

  def validateReturn(eclReturn: EclReturn): EitherT[Future, DataValidationErrorList, EclReturnSubmission] = EitherT {
    val validationResult = transformToEclReturnSubmission(eclReturn) match {
      case Valid(eclReturnSubmission) =>
        schemaValidator
          .validateAgainstJsonSchema(
            eclReturnSubmission,
            SchemaLoader.loadSchema("create-ecl-return-request.json")
          )
      case invalid                    => invalid
    }
    validationResult match {
      case Valid(eclReturnSubmission) => Future.successful(Right(eclReturnSubmission))
      case Invalid(validationError)   => Future.successful(Left(DataValidationErrorList(validationError.toList)))
    }
  }

  private def transformToEclReturnSubmission(eclReturn: EclReturn): ValidationResult[EclReturnSubmission] =
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
      validateOptExists(eclReturn.contactTelephoneNumber, "Contact telephone number"),
      validateOptExists(eclReturn.obligationDetails, "Obligation details")
    ).mapN(
      (
        _,
        relevantApLength,
        relevantApRevenue,
        calculatedLiability,
        carriedOutAmlRegulatedActivityForFullFy,
        amlRegulatedActivityLength,
        name,
        role,
        email,
        telephoneNumber,
        obligationDetails
      ) =>
        //TO DO - write apply method for EclReturnSubmission
        EclReturnSubmission(
          periodKey = obligationDetails.periodKey,
          returnDetails = EclReturnDetails(
            revenueBand = calculatedLiability.calculatedBand,
            amountOfEclDutyLiable = calculatedLiability.amountDue.amount,
            accountingPeriodRevenue = relevantApRevenue,
            accountingPeriodLength = relevantApLength.getOrElse(YearInDays),
            numberOfDaysRegulatedActivityTookPlace =
              if (carriedOutAmlRegulatedActivityForFullFy.contains(true)) Some(YearInDays)
              else amlRegulatedActivityLength,
            returnDate = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.now(clock))
          ),
          declarationDetails = DeclarationDetails(
            name = name,
            positionInCompany = role,
            emailAddress = email,
            telephoneNumber = telephoneNumber
          )
        )
    )

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => value.validNel
      case _           => DataValidationError.DataMissing(missingErrorMessage(description)).invalidNel
    }

  private def validateConditionalOptExists[T](
    optData: Option[T],
    condition: Boolean,
    description: String
  ): ValidationResult[Option[T]] =
    if (condition) {
      optData match {
        case Some(value) => Some(value).validNel
        case _           => DataValidationError.DataMissing(missingErrorMessage(description)).invalidNel

      }
    } else {
      None.validNel
    }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

}
