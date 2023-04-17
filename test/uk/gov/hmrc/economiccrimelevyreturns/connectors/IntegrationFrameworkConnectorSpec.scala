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
import org.mockito.ArgumentMatchers.any
import play.api.http.HeaderNames
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnDetails, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.{HttpClient, UpstreamErrorResponse}

import scala.concurrent.Future

class IntegrationFrameworkConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient                         = mock[HttpClient]
  val mockCorrelationIdGenerator: CorrelationIdGenerator = mock[CorrelationIdGenerator]
  val connector                                          = new IntegrationFrameworkConnector(appConfig, mockHttpClient, mockCorrelationIdGenerator)

  "submitEclReturn" should {
    "return either an error or the submit return response when the http client returns one" in forAll {
      (
        eclReturnDetails: EclReturnDetails,
        eitherResult: Either[UpstreamErrorResponse, SubmitEclReturnResponse],
        correlationId: String
      ) =>
        val expectedUrl =
          s"${appConfig.integrationFrameworkUrl}/economic-crime-levy/returns/$eclRegistrationReference/${eclReturnDetails.periodKey}"

        val expectedHeaders: Seq[(String, String)] = Seq(
          (HeaderNames.AUTHORIZATION, appConfig.integrationFrameworkBearerToken),
          (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
          (CustomHeaderNames.CorrelationId, correlationId)
        )

        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(
          mockHttpClient.POST[EclReturnDetails, Either[UpstreamErrorResponse, SubmitEclReturnResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(eclReturnDetails),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(eitherResult))

        val result = await(connector.submitEclReturn(eclRegistrationReference, eclReturnDetails))

        result shouldBe eitherResult

        verify(mockHttpClient, times(1))
          .POST[EclReturnDetails, Either[UpstreamErrorResponse, SubmitEclReturnResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(eclReturnDetails),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

}
