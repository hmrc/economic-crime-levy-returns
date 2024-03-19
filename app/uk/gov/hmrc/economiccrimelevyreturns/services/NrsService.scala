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
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.economiccrimelevyreturns.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.NrsSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.util.{Failure, Success, Try}
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class NrsService @Inject() (nrsConnector: NrsConnector, clock: Clock)(implicit
  ec: ExecutionContext
) {

  def submitToNrs(
    base64EncodedNrsSubmissionHtml: String,
    eclRegistrationReference: String,
    eventName: String
  )(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): EitherT[Future, NrsSubmissionError, NrsSubmissionResponse] =
    for {
      userAuthToken <- getAuthorizationHeader
      html          <- payloadSha256Checksum(base64EncodedNrsSubmissionHtml)
      headerData     = new JsObject(request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ",")))
      nrsSubmission  = NrsSubmission(
                         payload = base64EncodedNrsSubmissionHtml,
                         metadata = NrsMetadata(
                           userAuthToken,
                           headerData,
                           html,
                           NrsSearchKeys(eclRegistrationReference),
                           Instant.now(clock),
                           request.nrsIdentityData,
                           eventName
                         )
                       )
      nrsResponse   <- submit(nrsSubmission)
    } yield nrsResponse

  private def submit(
    nrsSubmission: NrsSubmission
  )(implicit hc: HeaderCarrier): EitherT[Future, NrsSubmissionError, NrsSubmissionResponse] =
    EitherT {
      nrsConnector
        .submitToNrs(nrsSubmission)
        .map { nrsSubmissionResponse =>
          Right(nrsSubmissionResponse)
        }
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(NrsSubmissionError.BadGateway(reason = s"NRS Submission Failed - $message", code = code))
          case NonFatal(thr) => Left(NrsSubmissionError.InternalUnexpectedError(Some(thr)))
        }
    }

  private def getAuthorizationHeader(implicit
    request: AuthorisedRequest[_]
  ): EitherT[Future, NrsSubmissionError, String] =
    EitherT {
      request.headers.get(HeaderNames.AUTHORIZATION) match {
        case Some(value) => Future.successful(Right(value))
        case None        =>
          Future.successful(
            Left(NrsSubmissionError.InternalUnexpectedError(None))
          )
      }
    }

  private def payloadSha256Checksum(
    base64EncodedNrsSubmissionHtml: String
  ): EitherT[Future, NrsSubmissionError, String] =
    EitherT {
      Try {
        val decodedHtml: String = new String(Base64.getDecoder.decode(base64EncodedNrsSubmissionHtml))

        MessageDigest
          .getInstance("SHA-256")
          .digest(decodedHtml.getBytes(StandardCharsets.UTF_8))
          .map("%02x".format(_))
          .mkString
      } match {
        case Success(html) => Future.successful(Right(html))
        case Failure(thr)  =>
          Future.successful(Left(NrsSubmissionError.InternalUnexpectedError(Some(thr))))
      }
    }

}
