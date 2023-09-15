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

import org.mockito.ArgumentMatchers
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class IntegrationFrameworkConnectorSpec extends SpecBase with BaseConnector {
  val mockHttpClient: HttpClientV2                       = mock[HttpClientV2]
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
        val expectedUrl =
          s"${appConfig.integrationFrameworkUrl}/economic-crime-levy/return/$eclRegistrationReference"

        val expectedHeaders: Seq[(String, String)] = Seq(
          (HeaderNames.AUTHORIZATION, s"Bearer ${appConfig.integrationFrameworkBearerToken}"),
          (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
          (CustomHeaderNames.CorrelationId, correlationId)
        )

        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(
          mockHttpClient
            .post(
              ArgumentMatchers.eq(url"$expectedUrl")
            )
            .setHeader(ArgumentMatchers.eq(expectedHeaders): _*)
            .withBody(Json.toJson(ArgumentMatchers.eq(eclReturnSubmission)))
            .executeAndDeserialise[SubmitEclReturnResponse]
        ).thenReturn(Future.successful(submitEclReturnResponse))

        val result = await(connector.submitEclReturn(eclRegistrationReference, eclReturnSubmission))

        result shouldBe submitEclReturnResponse
    }
  }

  "return UpstreamErrorResponse when call to integration framework returns an error" in forAll {
    (
      eclReturnSubmission: EclReturnSubmission,
      correlationId: String,
      submitEclReturnResponse: SubmitEclReturnResponse
    ) =>
      val expectedUrl =
        s"${appConfig.integrationFrameworkUrl}/economic-crime-levy/return/$eclRegistrationReference"

      val expectedHeaders: Seq[(String, String)] = Seq(
        (HeaderNames.AUTHORIZATION, s"Bearer ${appConfig.integrationFrameworkBearerToken}"),
        (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
        (CustomHeaderNames.CorrelationId, correlationId)
      )

      when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

      val upstreamErrorResponse = Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND))

      when(
        mockHttpClient
          .post(
            ArgumentMatchers.eq(url"$expectedUrl")
          )
          .setHeader(ArgumentMatchers.eq(expectedHeaders): _*)
          .withBody(Json.toJson(ArgumentMatchers.eq(eclReturnSubmission)))
          .executeAndDeserialise[SubmitEclReturnResponse]
      ).thenReturn(upstreamErrorResponse)

      val result = await(connector.submitEclReturn(eclRegistrationReference, eclReturnSubmission))

      result shouldBe upstreamErrorResponse
  }

}
