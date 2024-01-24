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

import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.DmsConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DmsSubmissionError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import java.time.Instant
import java.util.Base64
import scala.concurrent.Future

class DmsServiceSpec extends SpecBase {

  val mockDmsConnector: DmsConnector = mock[DmsConnector]
  val html                           = "<html><head></head><body></body></html>"
  val now                            = Instant.now

  val service = new DmsService(mockDmsConnector, appConfig)

  "submitToDms" should {
    "return correct value when the submission is successful" in {
      val encoded          = Base64.getEncoder.encodeToString(html.getBytes)
      val expectedResponse = HttpResponse.apply(ACCEPTED, "")

      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.successful(Right(expectedResponse)))

      val result = await(service.submitToDms(encoded, now).value)

      result shouldBe Right(SubmitEclReturnResponse(now, None))
    }

    "return DmsSubmissionError.BadGateway if submission fails" in forAll(generateErrorCode) { errorCode: Int =>
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val message = "Gateway Error"

      when(mockDmsConnector.sendPdf(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply(message, errorCode)))

      val result = await(service.submitToDms(encoded, now).value)

      result shouldBe Left(DmsSubmissionError.BadGateway(message, errorCode))
    }

    "return DmsSubmissionError.InternalUnexpectedError if an exception is thrown in sendPdf" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val exception = new Exception("Unexpected error")

      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.failed(exception))

      val result = await(service.submitToDms(encoded, now).value)

      result shouldBe Left(DmsSubmissionError.InternalUnexpectedError(Some(exception)))
    }
  }
}
