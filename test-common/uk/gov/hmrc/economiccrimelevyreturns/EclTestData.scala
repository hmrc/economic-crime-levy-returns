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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.generators.Generators
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.EclReturnDetails
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{CalculatedLiability, EclReturn, ObligationDetails}
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.{Clock, Instant, LocalDate}

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

final case class ValidNrsSubmission(
  base64EncodedNrsSubmissionHtml: String,
  eclRegistrationReference: String,
  businessPartnerId: String,
  nrsSubmission: NrsSubmission
)

trait EclTestData { self: Generators =>

  private val base64EncodedNrsSubmissionHtml  = "PGh0bWw+PHRpdGxlPkhlbGxvIFdvcmxkITwvdGl0bGU+PC9odG1sPg=="
  private val nrsSubmissionHtmlSha256Checksum = "38a8012d1af5587a9b37aef812810e31b2ddf7d405d20b5f1230a209d95c9d2b"

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

  implicit val arbCredentialRole: Arbitrary[CredentialRole] = Arbitrary {
    Gen.oneOf(User, Assistant)
  }

  implicit val arbAffinityGroup: Arbitrary[AffinityGroup] = Arbitrary {
    Gen.oneOf(Organisation, Individual, Agent)
  }

  implicit val arbUpstreamErrorResponse: Arbitrary[UpstreamErrorResponse] = Arbitrary {
    UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
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
          obligationDetails = Some(obligationDetails),
          base64EncodedNrsSubmissionHtml = Some(base64EncodedNrsSubmissionHtml)
        ),
      EclLiabilityCalculationData(
        relevantApLength = if (relevantAp12Months) FullYear else relevantApLength,
        relevantApRevenue = relevantApRevenue,
        amlRegulatedActivityLength =
          if (carriedOutAmlRegulatedActivityForFullFy) FullYear else amlRegulatedActivityLength
      ),
      EclReturnDetails(
        amountDue = calculatedLiability.amountDue.amount,
        periodKey = obligationDetails.periodKey
      )
    )
  }

  type AuthRetrievals =
    Option[String] ~ Enrolments ~ Option[String] ~ ConfidenceLevel ~ Option[String] ~ Option[String] ~
      Option[MdtpInformation] ~ Option[String] ~ LoginTimes ~
      Option[Credentials] ~ Option[Name] ~ Option[LocalDate] ~ Option[String] ~
      Option[AffinityGroup] ~ Option[String] ~ AgentInformation ~ Option[CredentialRole] ~ Option[String] ~
      Option[ItmpName] ~ Option[LocalDate] ~ Option[ItmpAddress]

  def arbAuthRetrievals(internalId: Option[String], enrolmentsWithEcl: Boolean): Arbitrary[AuthRetrievals] = Arbitrary {
    for {
      enrolments         <-
        Arbitrary.arbitrary[EnrolmentsWithEcl].map(e => if (enrolmentsWithEcl) e.enrolments else Enrolments(Set.empty))
      confidenceLevel    <- Arbitrary.arbitrary[ConfidenceLevel]
      externalId         <- Arbitrary.arbitrary[Option[String]]
      nino               <- Arbitrary.arbitrary[Option[String]]
      saUtr              <- Arbitrary.arbitrary[Option[String]]
      mdtpInformation    <- Arbitrary.arbitrary[Option[MdtpInformation]]
      credentialStrength <- Arbitrary.arbitrary[Option[String]]
      loginTimes         <- Arbitrary.arbitrary[LoginTimes]
      credentials        <- Arbitrary.arbitrary[Option[Credentials]]
      name               <- Arbitrary.arbitrary[Option[Name]]
      dateOfBirth        <- Arbitrary.arbitrary[Option[LocalDate]]
      email              <- Arbitrary.arbitrary[Option[String]]
      affinityGroup      <- Arbitrary.arbitrary[Option[AffinityGroup]]
      agentInformation   <- Arbitrary.arbitrary[AgentInformation]
      credentialRole     <- Arbitrary.arbitrary[Option[CredentialRole]]
      groupIdentifier    <- Arbitrary.arbitrary[Option[String]]
      itmpName           <- Arbitrary.arbitrary[Option[ItmpName]]
      itmpAddress        <- Arbitrary.arbitrary[Option[ItmpAddress]]
    } yield internalId and enrolments and externalId and confidenceLevel and nino and saUtr and
      mdtpInformation and credentialStrength and loginTimes and
      credentials and name and dateOfBirth and email and
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
            notableEvent = "ecl-registration",
            payloadContentType = MimeTypes.HTML,
            payloadSha256Checksum = nrsSubmissionHtmlSha256Checksum,
            userSubmissionTimestamp = Instant.now(clock),
            identityData = nrsIdentityData,
            userAuthToken = authorisedRequest.headers.get(HeaderNames.AUTHORIZATION).get,
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

}
