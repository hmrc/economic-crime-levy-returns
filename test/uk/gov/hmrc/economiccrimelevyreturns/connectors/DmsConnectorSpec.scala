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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import akka.stream.scaladsl.Source
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class DmsConnectorSpec extends SpecBase {
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val connector                          = new DmsConnector(
    mockHttpClient,
    appConfig,
    config,
    actorSystem
  )

  override def beforeEach(): Unit = {
    reset(mockRequestBuilder)
    reset(mockHttpClient)
  }

  "sendPdf" should {
    "return HttpResponse if post to DMS queue succeeds" in {

      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(ACCEPTED, "")))

      await(connector.sendPdf(Source(Seq.empty))) shouldBe ()

      verify(mockRequestBuilder, times(1))
        .execute(any(), any())
    }

    "return 4xx UpstreamErrorResponse if post to DMS queue returns an error response" in {
      val errorCode    = BAD_REQUEST
      val errorMessage = s"Upstream error code $errorCode"
      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, errorMessage)))

      Try(await(connector.sendPdf(Source(Seq.empty)))) match {
        case Failure(UpstreamErrorResponse(msg, _, _, _)) =>
          msg shouldEqual errorMessage
        case _                                            => fail("expected UpstreamErrorResponse when an error is received from DMS")
      }

      verify(mockRequestBuilder, times(1))
        .execute(any(), any())
    }

    "retry 3 times when a 5xx UpstreamErrorResponse is returned from post to DMS queue" in {
      val errorCode    = INTERNAL_SERVER_ERROR
      val errorMessage = s"Upstream error code $errorCode"
      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, errorMessage)))

      Try(await(connector.sendPdf(Source(Seq.empty)))) match {
        case Failure(UpstreamErrorResponse(msg, _, _, _)) =>
          msg shouldEqual errorMessage
        case _                                            => fail("expected UpstreamErrorResponse when an error is received from DMS")
      }

      verify(mockRequestBuilder, times(4))
        .execute(any(), any())
    }
  }
}
