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

import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject() (nrsConnector: NrsConnector, clock: Clock)(implicit
  ec: ExecutionContext
) extends Logging {

  def submitToNrs(
    optBase64EncodedNrsSubmissionHtml: Option[String],
    eclRegistrationReference: String
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): Future[NrsSubmissionResponse] = {
    val userAuthToken: String                  = request.headers.get(HeaderNames.AUTHORIZATION).get
    val headerData: JsObject                   = new JsObject(request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ",")))
    val base64EncodedNrsSubmissionHtml: String = optBase64EncodedNrsSubmissionHtml.getOrElse(
      throw new IllegalStateException("Base64 encoded NRS submission HTML not found in registration data")
    )

    val nrsSearchKeys: NrsSearchKeys = NrsSearchKeys(
      eclRegistrationReference = eclRegistrationReference
    )

    val nrsMetadata = NrsMetadata(
      businessId = "ecl",
      notableEvent = "ecl-registration",
      payloadContentType = MimeTypes.HTML,
      payloadSha256Checksum = payloadSha256Checksum(base64EncodedNrsSubmissionHtml),
      userSubmissionTimestamp = Instant.now(clock),
      identityData = request.nrsIdentityData,
      userAuthToken = userAuthToken,
      headerData = headerData,
      searchKeys = nrsSearchKeys
    )

    val nrsSubmission = NrsSubmission(
      payload = base64EncodedNrsSubmissionHtml,
      metadata = nrsMetadata
    )

    nrsConnector
      .submitToNrs(nrsSubmission)
      .map { nrsSubmissionResponse =>
        logger.info(s"Success response received from NRS with submission ID: ${nrsSubmissionResponse.nrSubmissionId}")
        nrsSubmissionResponse
      }
      .recover { case e: Throwable =>
        logger.error(s"Failed to send NRS submission after initial attempt and 3 retries: ${e.getMessage}")
        throw e
      }
  }

  private def payloadSha256Checksum(base64EncodedNrsSubmissionHtml: String): String = {
    val decodedHtml: String = new String(Base64.getDecoder.decode(base64EncodedNrsSubmissionHtml))

    MessageDigest
      .getInstance("SHA-256")
      .digest(decodedHtml.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_))
      .mkString
  }
}
