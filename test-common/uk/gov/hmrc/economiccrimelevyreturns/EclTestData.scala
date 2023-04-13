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

package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.derive.MkArbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.EclReturnDetails
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, ObligationDetails}

import java.time.{Instant, LocalDate}

case class ValidPeriodKey(periodKey: String)

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

final case class EclLiabilityCalculationData(
  relevantApLength: Int,
  relevantApRevenue: Long,
  amlRegulatedActivityLength: Int
)

final case class ValidEclReturn(
  eclReturn: EclReturn,
  eclLiabilityCalculationData: EclLiabilityCalculationData,
  expectedEclReturnDetails: EclReturnDetails
)

trait EclTestData { self: Generators =>

  val MinRevenue: Long = 0L
  val MaxRevenue: Long = 99999999999L
  val MinApDays: Int   = 1
  val MaxApDays: Int   = 999
  val MinAmlDays: Int  = 0
  val MaxAmlDays: Int  = 365
  val FullYear: Int    = 365

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbEnrolmentsWithEcl: Arbitrary[EnrolmentsWithEcl] = Arbitrary {
    for {
      enrolments               <- Arbitrary.arbitrary[Enrolments]
      enrolment                <- Arbitrary.arbitrary[Enrolment]
      eclRegistrationReference <- Arbitrary.arbitrary[String]
      eclEnrolmentIdentifier    = EnrolmentIdentifier(EclEnrolment.IdentifierKey, eclRegistrationReference)
      eclEnrolment              =
        enrolment.copy(key = EclEnrolment.ServiceName, identifiers = enrolment.identifiers :+ eclEnrolmentIdentifier)
    } yield EnrolmentsWithEcl(enrolments.copy(enrolments.enrolments + eclEnrolment))
  }

  implicit val arbEnrolmentsWithoutEcl: Arbitrary[EnrolmentsWithoutEcl] = Arbitrary {
    Arbitrary
      .arbitrary[Enrolments]
      .retryUntil(
        !_.enrolments.exists(e =>
          e.key == EclEnrolment.ServiceName && e.identifiers.exists(_.key == EclEnrolment.IdentifierKey)
        )
      )
      .map(EnrolmentsWithoutEcl)
  }

  implicit val arbEclReturn: Arbitrary[EclReturn] = Arbitrary {
    for {
      eclReturn  <- MkArbitrary[EclReturn].arbitrary.arbitrary
      internalId <- Gen.nonEmptyListOf(Arbitrary.arbitrary[Char]).map(_.mkString)
    } yield eclReturn.copy(internalId = internalId)
  }

  implicit val arbPeriodKey: Arbitrary[ValidPeriodKey] = Arbitrary {
    Gen.listOfN(4, Gen.alphaNumChar).map(_.mkString).map(ValidPeriodKey)
  }

  implicit val arbValidEclReturn: Arbitrary[ValidEclReturn] = Arbitrary {
    for {
      relevantAp12Months                      <- Arbitrary.arbitrary[Boolean]
      relevantApLength                        <- Gen.chooseNum[Int](MinApDays, MaxApDays)
      relevantApRevenue                       <- Gen.chooseNum[Long](MinRevenue, MaxRevenue)
      carriedOutAmlRegulatedActivityForFullFy <- Arbitrary.arbitrary[Boolean]
      amlRegulatedActivityLength              <- Gen.chooseNum[Int](MinAmlDays, MaxAmlDays)
      calculatedLiability                     <- Arbitrary.arbitrary[CalculatedLiability]
      contactName                             <- stringsWithMaxLength(160)
      contactRole                             <- stringsWithMaxLength(160)
      contactEmailAddress                     <- emailAddress(160)
      contactTelephoneNumber                  <- telephoneNumber(24)
      validPeriodKey                          <- Arbitrary.arbitrary[ValidPeriodKey]
      obligationDetails                       <- Arbitrary.arbitrary[ObligationDetails].map(_.copy(periodKey = validPeriodKey.periodKey))
      internalId                               = alphaNumericString
    } yield ValidEclReturn(
      EclReturn
        .empty(internalId = internalId)
        .copy(
          relevantAp12Months = Some(relevantAp12Months),
          relevantApLength = if (relevantAp12Months) None else Some(relevantApLength),
          relevantApRevenue = Some(relevantApRevenue),
          carriedOutAmlRegulatedActivityForFullFy = Some(carriedOutAmlRegulatedActivityForFullFy),
          amlRegulatedActivityLength =
            if (carriedOutAmlRegulatedActivityForFullFy) None else Some(amlRegulatedActivityLength),
          calculatedLiability = Some(calculatedLiability),
          contactName = Some(contactName),
          contactRole = Some(contactRole),
          contactEmailAddress = Some(contactEmailAddress),
          contactTelephoneNumber = Some(contactTelephoneNumber),
          obligationDetails = Some(obligationDetails)
        ),
      EclLiabilityCalculationData(
        relevantApLength = if (relevantAp12Months) FullYear else relevantApLength,
        relevantApRevenue = relevantApRevenue,
        amlRegulatedActivityLength =
          if (carriedOutAmlRegulatedActivityForFullFy) FullYear else amlRegulatedActivityLength
      ),
      EclReturnDetails(
        amountDue = calculatedLiability.amountDue,
        periodKey = obligationDetails.periodKey
      )
    )
  }

  def alphaNumericString: String = Gen.alphaNumStr.retryUntil(_.nonEmpty).sample.get

  val testInternalId: String               = alphaNumericString
  val testEclRegistrationReference: String = alphaNumericString

}
