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

package uk.gov.hmrc.economiccrimelevyreturns.models.nrs

import play.api.http.MimeTypes
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate}

final case class NrsSearchKeys(eclRegistrationReference: String)

object NrsSearchKeys {
  implicit val writes: OWrites[NrsSearchKeys] = Json.writes[NrsSearchKeys]
}

final case class NrsIdentityData(
  internalId: String,
  externalId: Option[String],
  agentCode: Option[String],
  credentials: Option[Credentials],
  confidenceLevel: Int,
  nino: Option[String],
  saUtr: Option[String],
  name: Option[Name],
  dateOfBirth: Option[LocalDate],
  email: Option[String],
  agentInformation: AgentInformation,
  groupIdentifier: Option[String],
  credentialRole: Option[CredentialRole],
  mdtpInformation: Option[MdtpInformation],
  itmpName: Option[ItmpName],
  itmpDateOfBirth: Option[LocalDate],
  itmpAddress: Option[ItmpAddress],
  affinityGroup: Option[AffinityGroup],
  credentialStrength: Option[String],
  loginTimes: LoginTimes
)

object NrsIdentityData {
  implicit val credentialsWrites: OWrites[Credentials]           = Json.writes[Credentials]
  implicit val nameWrites: OWrites[Name]                         = Json.writes[Name]
  implicit val agentInformationWrites: OWrites[AgentInformation] = Json.writes[AgentInformation]
  implicit val credentialRoleWrites: Writes[CredentialRole]      = (o: CredentialRole) => JsString(o.toString)
  implicit val mdtpInformationWrites: OWrites[MdtpInformation]   = Json.writes[MdtpInformation]
  implicit val itmpNameWrites: OWrites[ItmpName]                 = Json.writes[ItmpName]
  implicit val itmpAddressWrites: OWrites[ItmpAddress]           = Json.writes[ItmpAddress]
  implicit val affinityGroupWrites: Writes[AffinityGroup]        = (o: AffinityGroup) => JsString(o.toString)
  implicit val loginTimesWrites: OWrites[LoginTimes]             = Json.writes[LoginTimes]
  implicit val writes: OWrites[NrsIdentityData]                  = Json.writes[NrsIdentityData]
}

final case class NrsMetadata(
  businessId: String,
  notableEvent: String,
  payloadContentType: String,
  payloadSha256Checksum: String,
  userSubmissionTimestamp: Instant,
  identityData: NrsIdentityData,
  userAuthToken: String,
  headerData: JsObject,
  searchKeys: NrsSearchKeys
)

object NrsMetadata {
  def apply(
    userAuthToken: String,
    headerData: JsObject,
    payloadSha256Checksum: String,
    searchKeys: NrsSearchKeys,
    userSubmissionTimestamp: Instant,
    nrsIdentityData: NrsIdentityData,
    eventName: String
  ): NrsMetadata = NrsMetadata(
    businessId = "ecl",
    notableEvent = eventName,
    payloadContentType = MimeTypes.HTML,
    payloadSha256Checksum = payloadSha256Checksum,
    userSubmissionTimestamp = userSubmissionTimestamp,
    identityData = nrsIdentityData,
    userAuthToken = userAuthToken,
    headerData = headerData,
    searchKeys = searchKeys
  )

  implicit val userSubmissionTimestampWrites: Writes[Instant] = (instant: Instant) =>
    JsString(DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.MILLIS)))
  implicit val writes: OWrites[NrsMetadata]                   = Json.writes[NrsMetadata]
}

final case class NrsSubmission(payload: String, metadata: NrsMetadata)

object NrsSubmission {
  implicit val writes: OWrites[NrsSubmission] = Json.writes[NrsSubmission]
}
