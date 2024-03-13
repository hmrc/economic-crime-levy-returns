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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{BadGateway, DataRetrievalError, DataValidationError, DmsSubmissionError, InternalServiceError, NrsSubmissionError, ResponseError, ReturnsSubmissionError}

import scala.concurrent.{ExecutionContext, Future}

trait ErrorHandler extends Logging {

  implicit class ErrorConvertor[E, R](value: EitherT[Future, E, R]) {

    def asResponseError(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, ResponseError, R] =
      value.leftMap(c.convert).leftSemiflatTap {
        case InternalServiceError(message, _, cause) =>
          val causeText = cause
            .map { ex =>
              s"""
                   |Message: ${ex.getMessage}
                   |""".stripMargin
            }
            .getOrElse("No exception is available")
          logger.error(s"""Internal Server Error: $message
               |
               |$causeText""".stripMargin)
          Future.successful(())
        case BadGateway(message, _, responseCode)    =>
          val causeText = s"""
                 |Message: $message
                 |Upstream status code: $responseCode
                 |""".stripMargin

          logger.error(s"""Bad gateway: $message
               |
               |$causeText""".stripMargin)
          Future.successful(())
        case _                                       => Future.successful(())
      }
  }

  trait Converter[E] {
    def convert(error: E): ResponseError
  }

  implicit val nrsSubmissionErrorConverter: Converter[NrsSubmissionError] = new Converter[NrsSubmissionError] {
    def convert(error: NrsSubmissionError): ResponseError = error match {
      case NrsSubmissionError.BadGateway(cause, statusCode)  => ResponseError.badGateway(cause, statusCode)
      case NrsSubmissionError.InternalUnexpectedError(cause) =>
        ResponseError.internalServiceError(cause = cause)
    }
  }

  implicit val returnsSubmissionErrorConverter: Converter[ReturnsSubmissionError] =
    new Converter[ReturnsSubmissionError] {
      override def convert(error: ReturnsSubmissionError): ResponseError = error match {
        case ReturnsSubmissionError.BadGateway(cause, statusCode)  => ResponseError.badGateway(cause, statusCode)
        case ReturnsSubmissionError.InternalUnexpectedError(cause) =>
          ResponseError.internalServiceError(cause = cause)
      }
    }

  implicit val dmsSubmissionErrorConverter: Converter[DmsSubmissionError] =
    new Converter[DmsSubmissionError] {
      override def convert(error: DmsSubmissionError): ResponseError = error match {
        case DmsSubmissionError.BadGateway(cause, statusCode)  => ResponseError.badGateway(cause, statusCode)
        case DmsSubmissionError.InternalUnexpectedError(cause) =>
          ResponseError.internalServiceError(cause = cause)
      }
    }

  implicit val dataRetrievalErrorConverter: Converter[DataRetrievalError] =
    new Converter[DataRetrievalError] {
      override def convert(error: DataRetrievalError): ResponseError = error match {
        case DataRetrievalError.NotFound(id)                   => ResponseError.notFoundError(s"Unable to find record with id: $id")
        case DataRetrievalError.InternalUnexpectedError(cause) =>
          ResponseError.internalServiceError(cause = cause)
      }
    }

  implicit val dataValidationErrorConverter: Converter[DataValidationError] =
    new Converter[DataValidationError] {
      override def convert(value: DataValidationError): ResponseError = {
        val errorMessage = value match {
          case DataValidationError.SchemaValidationError(cause) =>
            s"""
             |Schema validation error: $cause
             |""".stripMargin
          case DataValidationError.DataMissing(cause)           =>
            s"""
             |Data missing: $cause
             |""".stripMargin
          case DataValidationError.DataInvalid(cause)           =>
            s"""
               |Data invalid: $cause
               |""".stripMargin
        }

        ResponseError.badRequestError(errorMessage)
      }
    }
}
