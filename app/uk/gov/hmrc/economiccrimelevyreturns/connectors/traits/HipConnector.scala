/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.connectors.traits

import play.api.Logging
import play.api.http.Status.*
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsResult, Reads}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.hip.Hip422Error
import uk.gov.hmrc.economiccrimelevyreturns.models.hip.HipPlatformErrors.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait HipConnector extends Logging {

  val appConfig: AppConfig

  def hipHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      HeaderNames.ACCEPT      -> MimeTypes.JSON,
      "correlationid"         -> correlationId,
      "X-Originating-System"  -> "ECL",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(
        Instant
          .now()
          .truncatedTo(ChronoUnit.SECONDS)
      ),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${appConfig.hipAuthorizationToken}"
    )

  // use only for 400 500 and 503
  def parseHipErrorEnvelopeResponse(response: HttpResponse): HipErrorTrait =
    if (!List(BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).contains(response.status)) {
      throw new IllegalArgumentException(s"Not a HIP error envelope status: ${response.status}")
    } else {
      Try(response.json.asOpt[HipErrorWrapper]).toOption.flatten match {
        case Some(HipErrorWrapper(_, sysErr: HipSystemErrorObject))    => sysErr
        case Some(HipErrorWrapper(_, hipFails: HipFailuresErrorArray)) => hipFails
        case _                                                         => HipUnexpectedError(response.status, response.body)
      }
    }

  private def originOf(response: HttpResponse): String =
    Try((response.json \ "origin").as[Origin].toString).getOrElse("unknown")

  /** Error statuses are never rewritten: retries and the service's error mapping both key off the original status. */
  def executeHip[T](requestBuilder: RequestBuilder, msg: String)(implicit
    ec: ExecutionContext,
    reads: Reads[T]
  ): Future[T] =
    requestBuilder
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK | CREATED                                              =>
            response.json
              .validate[T]
              .map(Future.successful)
              .recoverTotal(error => Future.failed(JsResult.Exception(error)))
          case BAD_REQUEST | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
            fail(
              response.status,
              s"origin: ${originOf(response)}, ${describe(parseHipErrorEnvelopeResponse(response))}",
              msg
            )
          case UNPROCESSABLE_ENTITY                                      =>
            fail(response.status, describe422(response), msg)
          case _                                                         =>
            fail(response.status, response.body, msg)
        }
      }

  private def describe(error: HipErrorTrait): String = error match {
    case HipSystemErrorObject(HipError(code, logID, message)) =>
      s"HIP system error code: $code, logID: $logID, message: $message"
    case HipFailuresErrorArray(failures)                      =>
      s"HIP failures: ${failures.map(f => s"${f.`type`} - ${f.reason}").mkString("[", "; ", "]")}"
    case HipUnexpectedError(status, body)                     =>
      s"HIP error response in unexpected format: status: $status body: $body"
  }

  private def describe422(response: HttpResponse): String =
    Try(response.json.asOpt[Hip422Error]).toOption.flatten
      .fold(s"HIP 422 in unexpected format: ${response.body}")(e =>
        s"HIP 422 errorId: ${e.error.errorId}, processingDate: ${e.error.processingDate}, text: ${e.error.text}"
      )

  private def fail[T](status: Int, text: String, msg: String): Future[T] = {
    logger.warn(s"$msg failed with status $status - $text")
    Future.failed(UpstreamErrorResponse(text, status))
  }
}
