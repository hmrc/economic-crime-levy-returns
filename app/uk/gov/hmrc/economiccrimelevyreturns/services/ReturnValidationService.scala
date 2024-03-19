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

import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Small
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyreturns.utils.{SchemaLoader, SchemaValidator}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.Future

class ReturnValidationService @Inject() (clock: Clock, schemaValidator: SchemaValidator) extends Logging {

  private val YearInDays: Int = 365

  def validateReturn(eclReturn: EclReturn): EitherT[Future, DataValidationError, EclReturnSubmission] =
    EitherT {
      transformToEclReturnSubmission(eclReturn) match {
        case Right(eclReturnSubmission) =>
          Future.successful(
            schemaValidator
              .validateAgainstJsonSchema(
                eclReturnSubmission,
                SchemaLoader.loadSchema("create-ecl-return-request.json")
              )
              .map(_ => eclReturnSubmission)
          )
        case Left(dataValidationError)  => Future.successful(Left(dataValidationError))
      }
    }

  private def transformToEclReturnSubmission(
    eclReturn: EclReturn
  ): Either[DataValidationError, EclReturnSubmission] =
    for {
      _                                       <- validateConditionalOptExists(
                                                   eclReturn.amendReason,
                                                   eclReturn.returnType.contains(AmendReturn),
                                                   "Amend reason"
                                                 )
      _                                       <- validateOptExists(eclReturn.relevantAp12Months, "Relevant AP 12 months choice")
      relevantApLength                        <- validateConditionalOptExists(
                                                   eclReturn.relevantApLength,
                                                   eclReturn.relevantAp12Months.contains(false),
                                                   "Relevant AP length"
                                                 )
      relevantApRevenue                       <- validateOptExists(eclReturn.relevantApRevenue, "Relevant AP revenue")
      calculatedLiability                     <- validateOptExists(eclReturn.calculatedLiability, "Calculated liability")
      carriedOutAmlRegulatedActivityForFullFy <- validateConditionalOptExists(
                                                   eclReturn.carriedOutAmlRegulatedActivityForFullFy,
                                                   eclReturn.calculatedLiability.isDefined && eclReturn.calculatedLiability.get.calculatedBand != Small,
                                                   "Carried out AML regulated activity for full FY choice"
                                                 )
      amlRegulatedActivityLength              <- validateConditionalOptExists(
                                                   eclReturn.amlRegulatedActivityLength,
                                                   eclReturn.carriedOutAmlRegulatedActivityForFullFy.contains(false),
                                                   "AML regulated activity length"
                                                 )
      name                                    <- validateOptExists(eclReturn.contactName, "Contact name")
      role                                    <- validateOptExists(eclReturn.contactRole, "Contact role")
      email                                   <- validateOptExists(eclReturn.contactEmailAddress, "Contact email address")
      telephoneNumber                         <- validateOptExists(eclReturn.contactTelephoneNumber, "Contact telephone number")
      obligationDetails                       <- validateOptExists(eclReturn.obligationDetails, "Obligation details")
    } yield EclReturnSubmission(
      obligationDetails.periodKey,
      EclReturnDetails(
        revenueBand = calculatedLiability.calculatedBand,
        amountOfEclDutyLiable = calculatedLiability.amountDue.amount,
        accountingPeriodRevenue = relevantApRevenue,
        accountingPeriodLength = relevantApLength.getOrElse(YearInDays),
        numberOfDaysRegulatedActivityTookPlace = if (carriedOutAmlRegulatedActivityForFullFy.contains(true)) {
          Some(YearInDays)
        } else { amlRegulatedActivityLength },
        returnDate = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.now(clock))
      ),
      declarationDetails = DeclarationDetails(name, role, email, telephoneNumber)
    )

  private def validateOptExists[T](
    data: Option[T],
    description: String
  ): Either[DataValidationError, T] =
    data match {
      case Some(value) => Right(value)
      case _           =>
        logger.info(s"Return Validation Failed - ${missingErrorMessage(description)}")
        Left(DataValidationError.DataMissing(missingErrorMessage(description)))
    }

  private def validateConditionalOptExists[T](
    data: Option[T],
    condition: Boolean,
    description: String
  ): Either[DataValidationError, Option[T]] =
    data match {
      case Some(value) if condition => Right(Some(value))
      case _ if !condition          => Right(None)
      case _                        =>
        logger.info(s"Return Validation Failed - ${missingErrorMessage(description)}")
        Left(DataValidationError.DataMissing(missingErrorMessage(description)))
    }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

}
