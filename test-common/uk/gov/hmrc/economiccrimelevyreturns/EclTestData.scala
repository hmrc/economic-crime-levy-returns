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

import org.scalacheck.{Arbitrary, Gen}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.Medium
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, ObligationDetails, SessionData}
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.{Clock, Instant, LocalDate}
import scala.math.BigDecimal.RoundingMode

case class ValidPeriodKey(periodKey: String)

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

final case class EclLiabilityCalculationData(
  relevantApLength: Int,
  relevantApRevenue: BigDecimal,
  amlRegulatedActivityLength: Int
)

final case class ValidEclReturn(
  eclReturn: EclReturn,
  eclLiabilityCalculationData: EclLiabilityCalculationData,
  expectedEclReturnSubmission: EclReturnSubmission
)

final case class ValidNrsSubmission(
  base64EncodedNrsSubmissionHtml: String,
  eclRegistrationReference: String,
  businessPartnerId: String,
  nrsSubmission: NrsSubmission
)

final case class ValidGetEclReturnSubmissionResponse(
  response: GetEclReturnSubmissionResponse
)

trait EclTestData { self: Generators =>

  private val base64EncodedNrsSubmissionHtml: String  = "PGh0bWw+PHRpdGxlPkhlbGxvIFdvcmxkITwvdGl0bGU+PC9odG1sPg=="
  private val nrsSubmissionHtmlSha256Checksum: String =
    "38a8012d1af5587a9b37aef812810e31b2ddf7d405d20b5f1230a209d95c9d2b"

  private val minRevenue: Double = 0.00
  private val maxRevenue: Double = 99999999999.99
  private val minApDays: Int     = 1
  private val maxApDays: Int     = 999
  private val minAmlDays: Int    = 0
  private val maxAmlDays: Int    = 365
  private val yearInDays: Int    = 365
  private val minAmountDue: Int  = 0
  private val maxAmountDue: Int  = 250000

  val base64EncodedDmsSubmissionHtml: String = "PGh0bWw+PHRpdGxlPkhlbGxvIFdvcmxkITwvdGl0bGU+PC9odG1sPg=="
  val uuidRegex: String                      = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbCredentialRole: Arbitrary[CredentialRole] = Arbitrary {
    Gen.oneOf(User, Assistant)
  }

  implicit val arbCredentials: Arbitrary[Credentials] = Arbitrary {
    for {
      providerId   <- Gen.alphaNumStr
      providerType <- Gen.alphaNumStr
    } yield Credentials(providerId, providerType)
  }

  implicit val arbName: Arbitrary[Name] = Arbitrary {
    for {
      name     <- Gen.option(Gen.alphaNumStr)
      lastName <- Gen.option(Gen.alphaNumStr)
    } yield Name(name, lastName)
  }

  implicit val arbAgentInformation: Arbitrary[AgentInformation] = Arbitrary {
    for {
      agentId           <- Gen.option(Gen.alphaNumStr)
      agentCode         <- Gen.option(Gen.alphaNumStr)
      agentFriendlyName <- Gen.option(Gen.alphaNumStr)
    } yield AgentInformation(agentId, agentCode, agentFriendlyName)
  }

  implicit val arbMdtpInformation: Arbitrary[MdtpInformation] = Arbitrary {
    for {
      deviceId  <- Gen.alphaNumStr
      sessionId <- Gen.alphaNumStr
    } yield MdtpInformation(deviceId, sessionId)
  }

  implicit val arbItmpName: Arbitrary[ItmpName] = Arbitrary {
    for {
      givenName  <- Gen.option(Gen.alphaNumStr)
      middleName <- Gen.option(Gen.alphaNumStr)
      familyName <- Gen.option(Gen.alphaNumStr)
    } yield ItmpName(givenName, middleName, familyName)
  }

  implicit val arbItmpAddress: Arbitrary[ItmpAddress] = Arbitrary {
    for {
      line1       <- Gen.option(Gen.alphaNumStr)
      line2       <- Gen.option(Gen.alphaNumStr)
      line3       <- Gen.option(Gen.alphaNumStr)
      line4       <- Gen.option(Gen.alphaNumStr)
      line5       <- Gen.option(Gen.alphaNumStr)
      postcode    <- Gen.option(Gen.alphaNumStr)
      countryName <- Gen.option(Gen.alphaNumStr)
      countryCode <- Gen.option(Gen.alphaNumStr)
    } yield ItmpAddress(line1, line2, line3, line4, line5, postcode, countryName, countryCode)
  }

  implicit val arbLoginTimes: Arbitrary[LoginTimes] = Arbitrary {
    for {
      currentLogin  <- arbInstant.arbitrary
      previousLogin <- Gen.option(arbInstant.arbitrary)
    } yield LoginTimes(currentLogin, previousLogin)
  }

  implicit lazy val arbJsObject: Arbitrary[JsObject] = Arbitrary {
    for {
      key   <- Gen.alphaNumStr
      value <- Gen.alphaNumStr
    } yield Json.obj(key -> value)
  }

  implicit lazy val arbNrsSearchKeys: Arbitrary[NrsSearchKeys] = Arbitrary {
    for {
      eclRegistrationReference <- Gen.alphaNumStr
    } yield NrsSearchKeys(eclRegistrationReference)
  }

  implicit val arbAffinityGroup: Arbitrary[AffinityGroup] = Arbitrary {
    Gen.oneOf(Organisation, Individual, Agent)
  }

  implicit val arbUpstreamErrorResponse: Arbitrary[UpstreamErrorResponse] = Arbitrary {
    UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
  }

  implicit lazy val arbEnrolmentIdentifier: Arbitrary[EnrolmentIdentifier] = Arbitrary {
    for {
      key   <- Gen.alphaNumStr
      value <- Gen.alphaNumStr
    } yield EnrolmentIdentifier(key, value)
  }

  implicit lazy val arbEnrolment: Arbitrary[Enrolment] = Arbitrary {
    for {
      key               <- Gen.alphaNumStr
      identifiers       <- Gen.listOf(arbEnrolmentIdentifier.arbitrary)
      state             <- Gen.oneOf("Activated", "NotYetActivated")
      delegatedAuthRule <- Gen.option(Gen.alphaNumStr)
    } yield Enrolment(key, identifiers, state, delegatedAuthRule)
  }

  implicit lazy val arbEnrolments: Arbitrary[Enrolments] = Arbitrary {
    Gen.listOf(arbEnrolment.arbitrary).map(es => Enrolments(es.toSet))
  }

  implicit val arbEnrolmentsWithEcl: Arbitrary[EnrolmentsWithEcl] = Arbitrary {
    for {
      enrolments               <- Arbitrary.arbitrary[Enrolments]
      enrolment                <- Arbitrary.arbitrary[Enrolment]
      eclRegistrationReference <- Arbitrary.arbitrary[String]
      eclEnrolmentIdentifier    = EnrolmentIdentifier(EclEnrolment.identifierKey, eclRegistrationReference)
      eclEnrolment              =
        enrolment.copy(key = EclEnrolment.serviceName, identifiers = enrolment.identifiers :+ eclEnrolmentIdentifier)
    } yield EnrolmentsWithEcl(enrolments.copy(enrolments.enrolments + eclEnrolment))
  }

  implicit val arbEnrolmentsWithoutEcl: Arbitrary[EnrolmentsWithoutEcl] = Arbitrary {
    Arbitrary
      .arbitrary[Enrolments]
      .retryUntil(
        !_.enrolments
          .exists(e => e.key == EclEnrolment.serviceName && e.identifiers.exists(_.key == EclEnrolment.identifierKey))
      )
      .map(EnrolmentsWithoutEcl)
  }

  implicit lazy val arbEclReturn: Arbitrary[EclReturn] = Arbitrary {
    for {
      internalId                              <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      relevantAp12Months                      <- Gen.option(Gen.oneOf(true, false))
      relevantApLength                        <- Gen.option(Gen.posNum[Int])
      relevantApRevenue                       <- Gen.option(bigDecimalInRange(minRevenue, maxRevenue))
      carriedOutAmlRegulatedActivityForFullFy <- Gen.option(Gen.oneOf(true, false))
      amlRegulatedActivityLength              <- Gen.option(Gen.posNum[Int])
      calculatedLiability                     <- Gen.option(arbCalculatedLiability.arbitrary)
      contactName                             <- Gen.option(Gen.alphaNumStr)
      contactRole                             <- Gen.option(Gen.alphaNumStr)
      contactEmailAddress                     <- Gen.option(Gen.alphaNumStr)
      contactTelephoneNumber                  <- Gen.option(Gen.alphaNumStr)
      obligationDetails                       <- Gen.option(arbObligationDetails.arbitrary)
      base64EncodedNrsSubmissionHtml          <- Gen.option(Gen.alphaNumStr)
      base64EncodedDmsSubmissionHtml          <- Gen.option(Gen.alphaNumStr)
      lastUpdated                             <- Gen.option(arbInstant.arbitrary)
      returnType                              <- Gen.option(arbReturnType.arbitrary)
      amendReason                             <- Gen.option(Gen.alphaNumStr)
    } yield EclReturn(
      internalId,
      relevantAp12Months,
      relevantApLength,
      relevantApRevenue,
      carriedOutAmlRegulatedActivityForFullFy,
      amlRegulatedActivityLength,
      calculatedLiability,
      contactName,
      contactRole,
      contactEmailAddress,
      contactTelephoneNumber,
      obligationDetails,
      base64EncodedNrsSubmissionHtml,
      base64EncodedDmsSubmissionHtml,
      lastUpdated,
      returnType,
      amendReason
    )
  }

  implicit val arbPeriodKey: Arbitrary[ValidPeriodKey] = Arbitrary {
    Gen.listOfN(4, Gen.alphaNumChar).map(_.mkString).map(p => ValidPeriodKey(p.toUpperCase))
  }

  implicit def arbBigDecimal(min: Double, max: Double): Arbitrary[BigDecimal] =
    Arbitrary {
      Gen.chooseNum[Double](min, max).map(BigDecimal.apply(_).setScale(2, RoundingMode.DOWN))
    }

  implicit lazy val arbObligationDetails: Arbitrary[ObligationDetails] = Arbitrary {
    for {
      status                            <- arbObligationStatus.arbitrary
      inboundCorrespondenceFromDate     <- arbLocalDate.arbitrary
      inboundCorrespondenceToDate       <- arbLocalDate.arbitrary
      inboundCorrespondenceDateReceived <- Gen.option(arbLocalDate.arbitrary)
      inboundCorrespondenceDueDate      <- arbLocalDate.arbitrary
      periodKey                         <- Gen.alphaNumStr
    } yield ObligationDetails(
      status,
      inboundCorrespondenceFromDate,
      inboundCorrespondenceToDate,
      inboundCorrespondenceDateReceived,
      inboundCorrespondenceDueDate,
      periodKey
    )
  }

  implicit val arbValidEclReturn: Arbitrary[ValidEclReturn] = Arbitrary {
    for {
      relevantAp12Months                      <- Gen.oneOf(true, false)
      relevantApLength                        <- Gen.chooseNum[Int](minApDays, maxApDays)
      relevantApRevenue                       <- arbBigDecimal(minRevenue, maxRevenue).arbitrary
      carriedOutAmlRegulatedActivityForFullFy <- Gen.oneOf(true, false)
      amlRegulatedActivityLength              <- Gen.chooseNum[Int](minAmlDays, maxAmlDays)
      liabilityAmountDue                      <- arbBigDecimal(minAmountDue, maxAmountDue).arbitrary
      calculatedLiability                     <-
        Arbitrary
          .arbitrary[CalculatedLiability]
          .map(calcLiability =>
            calcLiability
              .copy(calculatedBand = Medium, amountDue = calcLiability.amountDue.copy(amount = liabilityAmountDue))
          )
      contactName                             <- stringFromRegex(160, Regex.nameRegex)
      contactRole                             <- stringFromRegex(160, Regex.positionInCompanyRegex)
      contactEmailAddress                     <- emailAddress(160)
      contactTelephoneNumber                  <- stringFromRegex(24, Regex.telephoneNumberRegex)
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
          obligationDetails = Some(obligationDetails),
          base64EncodedNrsSubmissionHtml = Some(base64EncodedNrsSubmissionHtml)
        ),
      EclLiabilityCalculationData(
        relevantApLength = if (relevantAp12Months) yearInDays else relevantApLength,
        relevantApRevenue = relevantApRevenue,
        amlRegulatedActivityLength =
          if (carriedOutAmlRegulatedActivityForFullFy) yearInDays else amlRegulatedActivityLength
      ),
      EclReturnSubmission(
        periodKey = obligationDetails.periodKey,
        returnDetails = EclReturnDetails(
          revenueBand = calculatedLiability.calculatedBand,
          amountOfEclDutyLiable = calculatedLiability.amountDue.amount,
          accountingPeriodRevenue = relevantApRevenue,
          accountingPeriodLength = if (relevantAp12Months) yearInDays else relevantApLength,
          numberOfDaysRegulatedActivityTookPlace =
            if (carriedOutAmlRegulatedActivityForFullFy) Some(yearInDays) else Some(amlRegulatedActivityLength),
          returnDate = "2007-12-25"
        ),
        declarationDetails = DeclarationDetails(
          name = contactName,
          positionInCompany = contactRole,
          emailAddress = contactEmailAddress,
          telephoneNumber = contactTelephoneNumber
        )
      )
    )
  }

  type AuthRetrievals =
    Option[String] ~ ConfidenceLevel ~ Option[String] ~ Option[String] ~ Option[MdtpInformation] ~ Option[String] ~
      LoginTimes ~ Option[Credentials] ~ Option[LocalDate] ~ Option[String] ~ Option[AffinityGroup] ~ Option[String] ~
      AgentInformation ~ Option[CredentialRole] ~ Option[String] ~ Option[ItmpName] ~ Option[LocalDate] ~
      Option[ItmpAddress]

  implicit lazy val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
    Arbitrary(
      Gen.oneOf(
        ConfidenceLevel.L50,
        ConfidenceLevel.L200,
        ConfidenceLevel.L250,
        ConfidenceLevel.L500,
        ConfidenceLevel.L600
      )
    )

  def arbAuthNrsDataRetrievals(internalId: Option[String], enrolmentsWithEcl: Boolean): Arbitrary[AuthRetrievals] =
    Arbitrary {
      for {
        confidenceLevel    <- arbConfidenceLevel.arbitrary
        externalId         <- Arbitrary.arbitrary[Option[String]]
        nino               <- Arbitrary.arbitrary[Option[String]]
        saUtr              <- Arbitrary.arbitrary[Option[String]]
        mdtpInformation    <- Arbitrary.arbitrary[Option[MdtpInformation]]
        credentialStrength <- Arbitrary.arbitrary[Option[String]]
        loginTimes         <- Arbitrary.arbitrary[LoginTimes]
        credentials        <- Arbitrary.arbitrary[Option[Credentials]]
        dateOfBirth        <- Arbitrary.arbitrary[Option[LocalDate]]
        email              <- Arbitrary.arbitrary[Option[String]]
        affinityGroup      <- Arbitrary.arbitrary[Option[AffinityGroup]]
        agentInformation   <- Arbitrary.arbitrary[AgentInformation]
        credentialRole     <- Arbitrary.arbitrary[Option[CredentialRole]]
        groupIdentifier    <- Arbitrary.arbitrary[Option[String]]
        itmpName           <- Arbitrary.arbitrary[Option[ItmpName]]
        itmpAddress        <- Arbitrary.arbitrary[Option[ItmpAddress]]
      } yield externalId and confidenceLevel and nino and saUtr and
        mdtpInformation and credentialStrength and loginTimes and
        credentials and dateOfBirth and email and
        affinityGroup and agentInformation.agentCode and agentInformation and credentialRole and
        groupIdentifier and itmpName and dateOfBirth and itmpAddress
    }

  def arbValidNrsSubmission(request: FakeRequest[AnyContentAsEmpty.type], clock: Clock): Arbitrary[ValidNrsSubmission] =
    Arbitrary {
      for {
        eclRegistrationReference <- Arbitrary.arbitrary[String]
        businessPartnerId        <- Arbitrary.arbitrary[String]
        nrsIdentityData          <- Arbitrary.arbitrary[NrsIdentityData]
        authorisedRequest         =
          AuthorisedRequest(request, nrsIdentityData.internalId, eclRegistrationReference, nrsIdentityData)
      } yield ValidNrsSubmission(
        base64EncodedNrsSubmissionHtml = base64EncodedNrsSubmissionHtml,
        eclRegistrationReference = eclRegistrationReference,
        businessPartnerId = businessPartnerId,
        nrsSubmission = NrsSubmission(
          payload = base64EncodedNrsSubmissionHtml,
          metadata = NrsMetadata(
            businessId = "ecl",
            notableEvent = "ecl-return",
            payloadContentType = MimeTypes.HTML,
            payloadSha256Checksum = nrsSubmissionHtmlSha256Checksum,
            userSubmissionTimestamp = Instant.now(clock),
            identityData = nrsIdentityData,
            userAuthToken = authorisedRequest.headers.get(HeaderNames.AUTHORIZATION).getOrElse(""),
            headerData = new JsObject(authorisedRequest.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ","))),
            searchKeys = NrsSearchKeys(
              eclRegistrationReference = eclRegistrationReference
            )
          )
        )
      )
    }

  def alphaNumericString: String = Gen.alphaNumStr.retryUntil(_.nonEmpty).sample.get

  val testInternalId: String               = alphaNumericString
  val testEclRegistrationReference: String = alphaNumericString

  implicit val arbSessionData: Arbitrary[SessionData] =
    Arbitrary {
      for {
        id     <- Gen.uuid
        values <- Arbitrary.arbitrary[Map[String, String]]
      } yield SessionData(id.toString, values, None)
    }

  implicit val arbGetEclReturnChargeDetails: Arbitrary[GetEclReturnChargeDetails] = Arbitrary {
    for {
      chargeReference <- Gen.option(Gen.alphaNumStr)
      periodKey       <- Gen.alphaNumStr
      receiptDate     <- arbInstant.arbitrary
      returnType      <- Gen.alphaNumStr
    } yield GetEclReturnChargeDetails(chargeReference, periodKey, receiptDate, returnType)
  }

  implicit val arbGetEclReturnDeclarationDetails: Arbitrary[GetEclReturnDeclarationDetails] = Arbitrary {
    for {
      emailAddress      <- Gen.alphaNumStr
      name              <- Gen.alphaNumStr
      positionInCompany <- Gen.alphaNumStr
      telephoneNumber   <- Gen.alphaNumStr
    } yield GetEclReturnDeclarationDetails(emailAddress, name, positionInCompany, telephoneNumber)
  }

  implicit val arbGetEclReturnDetails: Arbitrary[GetEclReturnDetails] = Arbitrary {
    for {
      accountingPeriodLength                 <- Gen.posNum[Int]
      accountingPeriodRevenue                <- Gen.posNum[Double].map(BigDecimal.apply)
      amountOfEclDutyLiable                  <- Gen.posNum[Double].map(BigDecimal.apply)
      numberOfDaysRegulatedActivityTookPlace <- Gen.option(Gen.posNum[Int])
      returnDate                             <- arbLocalDate.arbitrary
      revenueBand                            <- arbBand.arbitrary
    } yield GetEclReturnDetails(
      accountingPeriodLength,
      accountingPeriodRevenue,
      amountOfEclDutyLiable,
      numberOfDaysRegulatedActivityTookPlace,
      returnDate,
      revenueBand
    )
  }

  implicit val arbValidGetEclReturnSubmissionResponse: Arbitrary[ValidGetEclReturnSubmissionResponse] = Arbitrary {
    for {
      accountingPeriodRevenue <- bigDecimalInRange(minRevenue, maxRevenue)
      amountOfEclDutyLiable   <- bigDecimalInRange(minAmountDue, maxAmountDue)
      chargeDetails           <- arbGetEclReturnChargeDetails.arbitrary
      declarationDetails      <- arbGetEclReturnDeclarationDetails.arbitrary
      eclReference            <- Arbitrary.arbitrary[String]
      processingDateTime      <- Arbitrary.arbitrary[Instant]
      returnDetails           <- arbGetEclReturnDetails.arbitrary
      submissionId            <- Arbitrary.arbitrary[String]
    } yield ValidGetEclReturnSubmissionResponse(
      GetEclReturnSubmissionResponse(
        chargeDetails = chargeDetails,
        declarationDetails = declarationDetails,
        eclReference = eclReference,
        processingDateTime = processingDateTime,
        returnDetails = returnDetails
          .copy(accountingPeriodRevenue = accountingPeriodRevenue, amountOfEclDutyLiable = amountOfEclDutyLiable),
        submissionId = Some(submissionId)
      )
    )
  }
}
