/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.errors._

class ErrorHandlerSpec extends SpecBase with ErrorHandler {

  "dataRetrievalErrorConverter" should {
    "return ResponseError.badGateway when DataRetrievalError.NotFound is converted" in forAll {
      (id: String) =>
        val dataRetrievalError = DataRetrievalError.NotFound(id)

        val result: ResponseError = dataRetrievalErrorConverter.convert(dataRetrievalError)

        result shouldBe ResponseError.notFoundError(s"Unable to find record with id: $id")
    }

    "return ResponseError.internalServiceError when DataRetrievalError.InternalUnexpectedError is converted" in {
      val dataRetrievalError = DataRetrievalError.InternalUnexpectedError(None)

      val result: ResponseError = dataRetrievalErrorConverter.convert(dataRetrievalError)

      result shouldBe ResponseError.internalServiceError(ErrorCode.InternalServerError)
    }
  }

  "dataValidationErrorConverter" should {
    "return ResponseError.standardError when DataValidationError.DataMissing is converted" in forAll {
      (errorMessage: String) =>
        val schemaValidationError = DataValidationError.DataMissing(errorMessage)

        val result: ResponseError = dataValidationErrorConverter.convert(schemaValidationError)

        result shouldBe StandardError(s"Data missing: $errorMessage", ErrorCode.BadRequest)
    }

    "return ResponseError.standardError when DataValidationError.DataInvalid is converted" in forAll {
      (errorMessage: String) =>
        val schemaValidationError = DataValidationError.DataInvalid(errorMessage)

        val result: ResponseError = dataValidationErrorConverter.convert(schemaValidationError)

        result shouldBe StandardError(s"Data invalid: $errorMessage", ErrorCode.BadRequest)
    }

    "return ResponseError.standardError when DataValidationError.SchemaValidationError is converted" in forAll {
      (errorMessage: String) =>
        val schemaValidationError = DataValidationError.SchemaValidationError(errorMessage)

        val result: ResponseError = dataValidationErrorConverter.convert(schemaValidationError)

        result shouldBe StandardError(s"Schema validation error: $errorMessage", ErrorCode.BadRequest)
    }
  }

  "dmsSubmissionErrorConverter" should {
    "return ResponseError.badGateway when DmsSubmissionError.BadGateway is converted" in forAll {
      (errorMessage: String) =>
      val dmsSubmissionError = DmsSubmissionError.BadGateway(errorMessage, BAD_GATEWAY)

      val result: ResponseError = dmsSubmissionErrorConverter.convert(dmsSubmissionError)

      result shouldBe ResponseError.badGateway(errorMessage, BAD_GATEWAY)
    }

    "return ResponseError.internalServiceError when DmsSubmissionError.InternalUnexpectedError is converted" in {
        val dmsSubmissionError = DmsSubmissionError.InternalUnexpectedError(None)

        val result: ResponseError = dmsSubmissionErrorConverter.convert(dmsSubmissionError)

        result shouldBe ResponseError.internalServiceError(ErrorCode.InternalServerError)
    }
  }

  "nrsSubmissionErrorConverter" should {
    "return ResponseError.badGateway when NrsSubmissionError.BadGateway is converted" in forAll {
      (errorMessage: String) =>
        val nrsSubmissionError = NrsSubmissionError.BadGateway(errorMessage, BAD_GATEWAY)

        val result: ResponseError = nrsSubmissionErrorConverter.convert(nrsSubmissionError)

        result shouldBe ResponseError.badGateway(errorMessage, BAD_GATEWAY)
    }

    "return ResponseError.internalServiceError when NrsSubmissionError.InternalUnexpectedError is converted" in {
      val nrsSubmissionError = NrsSubmissionError.InternalUnexpectedError(None)

      val result: ResponseError = nrsSubmissionErrorConverter.convert(nrsSubmissionError)

      result shouldBe ResponseError.internalServiceError(ErrorCode.InternalServerError)
    }
  }
}
