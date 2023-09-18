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
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class IntegrationFrameworkConnectorSpec extends SpecBase with BaseConnector {
  val mockHttpClient: HttpClientV2                       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder                 = mock[RequestBuilder]
  val mockCorrelationIdGenerator: CorrelationIdGenerator = mock[CorrelationIdGenerator]

  val connector = new IntegrationFrameworkConnector(appConfig, mockHttpClient, mockCorrelationIdGenerator)

  override def beforeEach(): Unit =
    reset(mockHttpClient)

  "submitEclReturn" should {
    "return submit return response when call to integration framework succeeds" in forAll {
      (
        eclReturnSubmission: EclReturnSubmission,
        correlationId: String,
        submitEclReturnResponse: SubmitEclReturnResponse
      ) =>
        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(submitEclReturnResponse)))))

        val result = await(connector.submitEclReturn(eclRegistrationReference, eclReturnSubmission))

        result shouldBe submitEclReturnResponse
    }
  }

  "return UpstreamErrorResponse when call to integration framework returns an error" in forAll {
    (
      eclReturnSubmission: EclReturnSubmission,
      correlationId: String
    ) =>
      when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

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
}
