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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.connectors.DmsConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DmsSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.utils.PdfGenerator.buildPdf
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

@Singleton
class DmsService @Inject() (
  dmsConnector: DmsConnector,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {

  def submitToDms(base64EncodedDmsSubmissionHtml: String, now: Instant)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DmsSubmissionError, SubmitEclReturnResponse] =
    for {
      pdf    <- generatePdf(base64EncodedDmsSubmissionHtml)
      date   <- getDateOfReceipt(now)
      result <- submitPdfToDms(date, pdf, now)
    } yield result

  private def generatePdf(
    base64EncodedNrsSubmissionHtml: String
  ): EitherT[Future, DmsSubmissionError, ByteArrayOutputStream] =
    EitherT {
      Try {
        val decodedHtml: String = new String(Base64.getDecoder.decode(base64EncodedNrsSubmissionHtml))
        buildPdf(decodedHtml)
      } match {
        case Success(pdf) => Future.successful(Right(pdf))
        case Failure(thr) =>
          Future.successful(Left(DmsSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr))))
      }
    }

  private def submitPdfToDms(dateOfReceipt: String, pdf: ByteArrayOutputStream, now: Instant)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DmsSubmissionError, SubmitEclReturnResponse] =
    EitherT {
      val body = createBody(dateOfReceipt, pdf)
      dmsConnector.sendPdf(body).map(_ => Right(SubmitEclReturnResponse(now, None))).recover {
        case error @ UpstreamErrorResponse(message, code, _, _)
            if UpstreamErrorResponse.Upstream5xxResponse
              .unapply(error)
              .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          Left(DmsSubmissionError.BadGateway(reason = message, code = code))
        case NonFatal(thr) => Left(DmsSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
      }
    }

  private def createBody(dateOfReceipt: String, pdf: ByteArrayOutputStream) =
    Source(
      Seq(
        DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
        DataPart("metadata.source", appConfig.dmsSubmissionSource),
        DataPart("metadata.timeOfReceipt", dateOfReceipt),
        DataPart("metadata.formId", appConfig.dmsSubmissionFormId),
        DataPart("metadata.customerId", appConfig.dmsSubmissionCustomerId),
        DataPart("metadata.classificationType", appConfig.dmsSubmissionClassificationType),
        DataPart("metadata.businessArea", appConfig.dmsSubmissionBusinessArea),
        FilePart(
          key = "form",
          filename = "form.pdf",
          contentType = Some("application/pdf"),
          ref = Source.single(ByteString(pdf.toByteArray))
        )
      )
    )

  private def getDateOfReceipt(now: Instant): EitherT[Future, DmsSubmissionError, String] =
    EitherT {
      Try {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
          LocalDateTime.ofInstant(now.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
        )
      } match {
        case Success(date) => Future.successful(Right(date))
        case Failure(thr)  =>
          Future.successful(Left(DmsSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr))))
      }
    }

}
