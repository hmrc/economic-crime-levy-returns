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

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, GetEclReturnResponse, SubmitEclReturnResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class IntegrationFrameworkConnectorSpec extends SpecBase with BaseConnector {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val connector = new IntegrationFrameworkConnector(appConfig, mockHttpClient, config, actorSystem)

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "getEclReturn" should {
    "return submit return response when call to integration framework succeeds" in forAll {
      (
        getEclReturnResponse: GetEclReturnResponse
      ) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(getEclReturnResponse)))))

        val result = await(connector.getEclReturn(periodKey, eclRegistrationReference))

        result shouldBe getEclReturnResponse
    }
  }

  "submitEclReturn" should {
    "return submit return response when call to integration framework succeeds" in forAll {
      (
        eclReturnSubmission: EclReturnSubmission,
        submitEclReturnResponse: SubmitEclReturnResponse
      ) =>
        beforeEach()

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(submitEclReturnResponse)))))

        val result = await(connector.submitEclReturn(eclRegistrationReference, eclReturnSubmission))

        result shouldBe submitEclReturnResponse
    }

    "return 4xx UpstreamErrorResponse when call to integration framework returns an error" in forAll {
      (
        eclReturnSubmission: EclReturnSubmission
      ) =>
        beforeEach()

        val errorCode = UNAUTHORIZED

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Failed authorization")))

        Try(await(connector.submitEclReturn(eclRegistrationReference, eclReturnSubmission))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from DMS")
        }
    }

    "return 5xx UpstreamErrorResponse when call to integration framework returns an error and executes retry" in forAll {
      (
        eclReturnSubmission: EclReturnSubmission
      ) =>
        beforeEach()

        val errorCode = INTERNAL_SERVER_ERROR

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(errorCode, "Internal server error")))

        Try(await(connector.submitEclReturn(eclRegistrationReference, eclReturnSubmission))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received from DMS")
        }

        verify(mockRequestBuilder, times(4))
          .execute(any(), any())
    }
  }
}
