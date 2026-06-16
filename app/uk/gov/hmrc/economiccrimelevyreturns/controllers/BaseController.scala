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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SessionData}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{GetEclReturnSubmissionResponse, SubmitEclReturnResponse}

import scala.concurrent.{ExecutionContext, Future}

trait BaseController {

  def checkOptionalValueExists[T](value: Option[T]): EitherT[Future, ResponseError, T] = EitherT(
    Future.successful(
      value match {
        case Some(value) => Right(value)
        case None        => Left(ResponseError.internalServiceError())
      }
    )
  )

  implicit class ResponseHandler[R](value: EitherT[Future, ResponseError, R]) {

    def convertToResult(responseCode: Int)(implicit c: ResultsConverter[R], ec: ExecutionContext): Future[Result] =
      value.fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        response => c.getResponseWithCode(response, responseCode)
      )
  }

  trait ResultsConverter[R] {
    def getResponseWithCode(response: R, responseCode: Int): Result
  }

  implicit val submitEclReturn: ResultsConverter[EclReturn] =
    new ResultsConverter[EclReturn] {
      override def getResponseWithCode(response: EclReturn, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }

  implicit val submitEclReturnResponse: ResultsConverter[SubmitEclReturnResponse] =
    new ResultsConverter[SubmitEclReturnResponse] {
      override def getResponseWithCode(response: SubmitEclReturnResponse, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }

  implicit val unitResponse: ResultsConverter[Unit] =
    new ResultsConverter[Unit] {
      override def getResponseWithCode(response: Unit, responseCode: Int): Result =
        Status(responseCode)
    }

  implicit val sessionDataResponse: ResultsConverter[SessionData] =
    new ResultsConverter[SessionData] {
      override def getResponseWithCode(response: SessionData, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }

  implicit val getEclReturnResponse: ResultsConverter[GetEclReturnSubmissionResponse] =
    new ResultsConverter[GetEclReturnSubmissionResponse] {
      override def getResponseWithCode(response: GetEclReturnSubmissionResponse, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }
}
