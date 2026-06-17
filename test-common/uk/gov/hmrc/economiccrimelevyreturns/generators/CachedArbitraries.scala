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

package uk.gov.hmrc.economiccrimelevyreturns.generators

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.economiccrimelevyreturns.models.des.{Fulfilled => DesFulfilled, ObligationStatus => DesObligationStatus, Open => DesOpen}
import uk.gov.hmrc.economiccrimelevyreturns.models.*
import uk.gov.hmrc.economiccrimelevyreturns.models.dms.*
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.*
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ErrorCode

object CachedArbitraries extends EclTestData with Generators {

  implicit lazy val arbBand: Arbitrary[Band]                                       = Arbitrary(Gen.oneOf(Band.Small, Band.Medium, Band.Large, Band.VeryLarge))
  implicit lazy val arbObligationStatus: Arbitrary[ObligationStatus]               = Arbitrary(Gen.oneOf(Open, Fulfilled))
  implicit lazy val arbDesObligationStatus: Arbitrary[DesObligationStatus]         = Arbitrary(Gen.oneOf(DesOpen, DesFulfilled))
  implicit lazy val arbCalculatedLiability: Arbitrary[CalculatedLiability]         = Arbitrary {
    for {
      amountDue      <- arbEclAmount.arbitrary
      bands          <- arbBands.arbitrary
      calculatedBand <- arbBand.arbitrary
    } yield CalculatedLiability(amountDue, bands, calculatedBand)
  }
  implicit lazy val arbSubmitEclReturnResponse: Arbitrary[SubmitEclReturnResponse] = Arbitrary {
    for {
      processingDate  <- arbInstant.arbitrary
      chargeReference <- Gen.option(Gen.alphaNumStr)
    } yield SubmitEclReturnResponse(processingDate, chargeReference)
  }
  implicit lazy val arbEclReturnSubmission: Arbitrary[EclReturnSubmission]         = Arbitrary {
    for {
      periodKey          <- Gen.alphaNumStr
      returnDetails      <- arbEclReturnDetails.arbitrary
      declarationDetails <- arbDeclarationDetails.arbitrary
    } yield EclReturnSubmission(periodKey, returnDetails, declarationDetails)
  }
  implicit lazy val arbNrsIdentityData: Arbitrary[NrsIdentityData]                 = Arbitrary {
    for {

      internalId         <- Gen.alphaNumStr
      externalId         <- Gen.option(Gen.alphaNumStr)
      agentCode          <- Gen.option(Gen.alphaNumStr)
      credentials        <- Gen.option(arbCredentials.arbitrary)
      confidenceLevel    <- Gen.posNum[Int]
      nino               <- Gen.option(Gen.alphaNumStr)
      saUtr              <- Gen.option(Gen.alphaNumStr)
      name               <- Gen.option(arbName.arbitrary)
      dateOfBirth        <- Gen.option(arbLocalDate.arbitrary)
      email              <- Gen.option(Gen.alphaNumStr)
      agentInformation   <- arbAgentInformation.arbitrary
      groupIdentifier    <- Gen.option(Gen.alphaNumStr)
      credentialRole     <- Gen.option(arbCredentialRole.arbitrary)
      mdtpInformation    <- Gen.option(arbMdtpInformation.arbitrary)
      itmpName           <- Gen.option(arbItmpName.arbitrary)
      itmpDateOfBirth    <- Gen.option(arbLocalDate.arbitrary)
      itmpAddress        <- Gen.option(arbItmpAddress.arbitrary)
      affinityGroup      <- Gen.option(arbAffinityGroup.arbitrary)
      credentialStrength <- Gen.option(Gen.alphaNumStr)
      loginTimes         <- arbLoginTimes.arbitrary

    } yield NrsIdentityData(
      internalId,
      externalId,
      agentCode,
      credentials,
      confidenceLevel,
      nino,
      saUtr,
      name,
      dateOfBirth,
      email,
      agentInformation,
      groupIdentifier,
      credentialRole,
      mdtpInformation,
      itmpName,
      itmpDateOfBirth,
      itmpAddress,
      affinityGroup,
      credentialStrength,
      loginTimes
    )
  }
  implicit lazy val arbNrsSubmission: Arbitrary[NrsSubmission]                     = Arbitrary {
    for {
      payload  <- Gen.alphaNumStr
      metadata <- arbNrsMetadata.arbitrary
    } yield NrsSubmission(payload, metadata)
  }
  implicit lazy val arbNrsSubmissionResponse: Arbitrary[NrsSubmissionResponse]     = Arbitrary {
    for {
      nrSubmissionId <- Gen.alphaNumStr
    } yield NrsSubmissionResponse(nrSubmissionId)
  }
  implicit lazy val arbDmsNotification: Arbitrary[DmsNotification]                 = Arbitrary {
    for {
      id            <- Gen.alphaNumStr
      status        <- Gen.oneOf(SubmissionItemStatus.values)
      failureReason <- Gen.option(Gen.alphaNumStr)
    } yield DmsNotification(id, status, failureReason)
  }
  implicit lazy val arbErrorCode: Arbitrary[ErrorCode]                             = Arbitrary(Gen.oneOf(ErrorCode.errorCodes))
  implicit lazy val arbReturnType: Arbitrary[ReturnType]                           = Arbitrary(Gen.oneOf(FirstTimeReturn, AmendReturn))

  implicit lazy val arbEclAmount: Arbitrary[EclAmount] = Arbitrary {
    for {
      amount      <- bigDecimalInRange(0, Double.MaxValue)
      apportioned <- Gen.oneOf(true, false)
    } yield EclAmount(amount, apportioned)
  }

  implicit lazy val arbBands: Arbitrary[Bands] = Arbitrary {
    for {
      small       <- arbBandRange.arbitrary
      medium      <- arbBandRange.arbitrary
      large       <- arbBandRange.arbitrary
      veryLarge   <- arbBandRange.arbitrary
      apportioned <- Gen.oneOf(true, false)
    } yield Bands(small, medium, large, veryLarge, apportioned)
  }

  implicit lazy val arbBandRange: Arbitrary[BandRange] = Arbitrary {
    for {
      from   <- Gen.posNum[Long]
      to     <- Gen.posNum[Long]
      amount <- Gen.posNum[Double].map(BigDecimal.apply)
    } yield BandRange(from, to, amount)
  }

  implicit lazy val arbEclReturnDetails: Arbitrary[EclReturnDetails] = Arbitrary {
    for {
      revenueBand                            <- arbBand.arbitrary
      amountOfEclDutyLiable                  <- bigDecimalInRange(0, Double.MaxValue)
      accountingPeriodRevenue                <- bigDecimalInRange(0, Double.MaxValue)
      accountingPeriodLength                 <- Gen.posNum[Int]
      numberOfDaysRegulatedActivityTookPlace <- Gen.option(Gen.posNum[Int])
      returnDate                             <- Gen.alphaNumStr
    } yield EclReturnDetails(
      revenueBand,
      amountOfEclDutyLiable,
      accountingPeriodRevenue,
      accountingPeriodLength,
      numberOfDaysRegulatedActivityTookPlace,
      returnDate
    )
  }

  implicit lazy val arbDeclarationDetails: Arbitrary[DeclarationDetails] = Arbitrary {
    for {
      name              <- Gen.alphaNumStr
      positionInCompany <- Gen.alphaNumStr
      emailAddress      <- Gen.alphaNumStr
      telephoneNumber   <- Gen.alphaNumStr
    } yield DeclarationDetails(name, positionInCompany, emailAddress, telephoneNumber)
  }

  implicit lazy val arbNrsMetadata: Arbitrary[NrsMetadata] = Arbitrary {
    for {
      businessId              <- Gen.alphaNumStr
      notableEvent            <- Gen.alphaNumStr
      payloadContentType      <- Gen.alphaNumStr
      payloadSha256Checksum   <- Gen.alphaNumStr
      userSubmissionTimestamp <- arbInstant.arbitrary
      identityData            <- arbNrsIdentityData.arbitrary
      userAuthToken           <- Gen.alphaNumStr
      headerData              <- arbJsObject.arbitrary
      searchKeys              <- arbNrsSearchKeys.arbitrary
    } yield NrsMetadata(
      businessId,
      notableEvent,
      payloadContentType,
      payloadSha256Checksum,
      userSubmissionTimestamp,
      identityData,
      userAuthToken,
      headerData,
      searchKeys
    )
  }

}
