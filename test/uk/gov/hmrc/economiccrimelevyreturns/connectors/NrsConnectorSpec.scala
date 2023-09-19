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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.{NrsSubmission, NrsSubmissionResponse}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class NrsConnectorSpec extends SpecBase {

  val actorSystem: ActorSystem           = ActorSystem("test")
  val config: Config                     = app.injector.instanceOf[Config]
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new NrsConnector(appConfig, mockHttpClient, config, actorSystem)
  val nrsSubmissionUrl                   = url"${appConfig.nrsBaseUrl}/submission"

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "submitToNrs" should {
    "return a NRS submission response when the http client returns a NRS submission response" in forAll {
      (nrsSubmission: NrsSubmission, nrsSubmissionResponse: NrsSubmissionResponse) =>
        beforeEach()

        when(mockHttpClient.post(ArgumentMatchers.eq(nrsSubmissionUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(nrsSubmissionResponse)))))

        val result = await(connector.submitToNrs(nrsSubmission))

        result shouldBe nrsSubmissionResponse

        verify(mockRequestBuilder, times(1))
          .execute(any(), any())
    }

    "return 4xx UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (
        nrsSubmission: NrsSubmission
      ) =>
        beforeEach()

        val errorCode = NOT_FOUND

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Failed authorization")))

        Try(await(connector.submitToNrs(nrsSubmission))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from NRS")
        }

        verify(mockRequestBuilder, times(1))
          .execute(any(), any())
    }
  }

  "retry 3 times when a 5xx UpstreamErrorResponse is returned from integration framework" in forAll {
    (
      nrsSubmission: NrsSubmission
    ) =>
      beforeEach()

      val errorCode = INTERNAL_SERVER_ERROR

      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

      Try(await(connector.submitToNrs(nrsSubmission))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received from NRS")
      }

      verify(mockRequestBuilder, times(4))
        .execute(any(), any())
  }

}
