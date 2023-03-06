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
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn}

import java.time.Instant

final case class ValidEclReturn(eclReturn: EclReturn)

trait EclTestData { self: Generators =>

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbEclReturn: Arbitrary[EclReturn] = Arbitrary {
    for {
      eclReturn  <- MkArbitrary[EclReturn].arbitrary.arbitrary
      internalId <- Gen.nonEmptyListOf(Arbitrary.arbitrary[Char]).map(_.mkString)
    } yield eclReturn.copy(internalId = internalId)
  }

  implicit val arbValidEclReturn: Arbitrary[ValidEclReturn] = Arbitrary {
    for {
      relevantAp12Months                      <- Arbitrary.arbitrary[Boolean]
      relevantApLength                        <- Arbitrary.arbitrary[Int]
      relevantApRevenue                       <- Arbitrary.arbitrary[Long]
      carriedOutAmlRegulatedActivityForFullFy <- Arbitrary.arbitrary[Boolean]
      amlRegulatedActivityLength              <- Arbitrary.arbitrary[Int]
      calculatedLiability                     <- Arbitrary.arbitrary[CalculatedLiability]
      contactName                             <- stringsWithMaxLength(160)
      contactRole                             <- stringsWithMaxLength(160)
      contactEmailAddress                     <- emailAddress(160)
      contactTelephoneNumber                  <- telephoneNumber(24)
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
          contactTelephoneNumber = Some(contactTelephoneNumber)
        )
    )
  }

  def alphaNumericString: String = Gen.alphaNumStr.retryUntil(_.nonEmpty).sample.get

}
