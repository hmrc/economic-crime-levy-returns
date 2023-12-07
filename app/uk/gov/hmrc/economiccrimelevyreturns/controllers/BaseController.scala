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
import play.api.mvc.Results.{Ok, Status}
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, SessionData}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse

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

    def convertToResult(implicit c: Converter[R], ec: ExecutionContext): Future[Result] =
      value.fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        response => c.getResponse(response)
      )

    def convertToResult(responseCode: Int)(implicit c: Converter[R], ec: ExecutionContext): Future[Result] =
      value.fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        response => c.getResponseWithCode(response, responseCode)
      )
  }

  trait Converter[R] {
    def getResponse(response: R): Result

    def getResponseWithCode(response: R, responseCode: Int): Result
  }

  implicit val submitEclReturn: Converter[EclReturn] =
    new Converter[EclReturn] {
      override def getResponse(response: EclReturn) = Ok(Json.toJson(response))

      override def getResponseWithCode(response: EclReturn, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }

  implicit val submitEclReturnResponse: Converter[SubmitEclReturnResponse] =
    new Converter[SubmitEclReturnResponse] {
      override def getResponse(response: SubmitEclReturnResponse) = Ok(Json.toJson(response))

      override def getResponseWithCode(response: SubmitEclReturnResponse, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }

  implicit val unitResponse: Converter[Unit] =
    new Converter[Unit] {
      override def getResponse(response: Unit) = Ok

      override def getResponseWithCode(response: Unit, responseCode: Int): Result =
        Ok
    }

  implicit val sessionDataResponse: Converter[SessionData] =
    new Converter[SessionData] {
      override def getResponse(response: SessionData): Result = Ok(Json.toJson(response))

      override def getResponseWithCode(response: SessionData, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }
}
