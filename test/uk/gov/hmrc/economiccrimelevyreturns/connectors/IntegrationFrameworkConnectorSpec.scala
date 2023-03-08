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
import uk.gov.hmrc.economiccrimelevyreturns.models.des.ObligationData
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnDetails, FinancialDetails, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class IntegrationFrameworkConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient                         = mock[HttpClient]
  val mockCorrelationIdGenerator: CorrelationIdGenerator = mock[CorrelationIdGenerator]
  val connector                                          = new IntegrationFrameworkConnector(appConfig, mockHttpClient, mockCorrelationIdGenerator)

  "getFinancialDetails" should {
    "return financial details when the http client returns financial details" in forAll {
      (eclRegistrationReference: String, financialDetails: FinancialDetails, correlationId: String) =>
        val expectedUrl =
          s"${appConfig.integrationFrameworkUrl}/enterprise/02.00.00/financial-data/zecl/$eclRegistrationReference/ECL"

        val expectedHeaders: Seq[(String, String)] = Seq(
          (HeaderNames.AUTHORIZATION, appConfig.integrationFrameworkBearerToken),
          (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
          (CustomHeaderNames.CorrelationId, correlationId)
        )

        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(
          mockHttpClient.GET[FinancialDetails](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any())
        )
          .thenReturn(Future.successful(financialDetails))

        val result = await(connector.getFinancialDetails(eclRegistrationReference))

        result shouldBe financialDetails

        verify(mockHttpClient, times(1))
          .GET[ObligationData](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }

  "submitEclReturn" should {
    "return an ECL return reference when the http client returns an ECL return reference" in forAll {
      (
        eclReturnDetails: EclReturnDetails,
        submitEclReturnResponse: SubmitEclReturnResponse,
        correlationId: String
      ) =>
        val periodKey = "22XY"

        val expectedUrl =
          s"${appConfig.integrationFrameworkUrl}/economic-crime-levy/returns/$eclRegistrationReference/$periodKey"

        val expectedHeaders: Seq[(String, String)] = Seq(
          (HeaderNames.AUTHORIZATION, appConfig.integrationFrameworkBearerToken),
          (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
          (CustomHeaderNames.CorrelationId, correlationId)
        )

        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(
          mockHttpClient.POST[EclReturnDetails, SubmitEclReturnResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(eclReturnDetails),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(submitEclReturnResponse))

        val result = await(connector.submitEclReturn(eclRegistrationReference, eclReturnDetails))

        result shouldBe submitEclReturnResponse

        verify(mockHttpClient, times(1))
          .POST[EclReturnDetails, SubmitEclReturnResponse](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(eclReturnDetails),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

}
