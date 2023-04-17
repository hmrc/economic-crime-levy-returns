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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnDetails, SubmitEclReturnResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class ReturnServiceSpec extends SpecBase {
  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]
  val mockAuditConnector: AuditConnector                               = mock[AuditConnector]

  val service = new ReturnService(mockIntegrationFrameworkConnector, mockAuditConnector)

  "submitEclReturn" should {
    "trigger an audit event and return the successful submit return response" in forAll {
      (
        eclReturn: EclReturn,
        eclReturnDetails: EclReturnDetails,
        returnResponse: SubmitEclReturnResponse
      ) =>
        when(
          mockIntegrationFrameworkConnector
            .submitEclReturn(ArgumentMatchers.eq(eclRegistrationReference), ArgumentMatchers.eq(eclReturnDetails))(
              any()
            )
        )
          .thenReturn(Future.successful(Right(returnResponse)))

        val result = await(service.submitEclReturn(eclRegistrationReference, eclReturnDetails, eclReturn))

        result shouldBe returnResponse

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "trigger an audit event and throw an exception when submitting the return fails" in forAll {
      (
        eclReturn: EclReturn,
        eclReturnDetails: EclReturnDetails
      ) =>
        when(
          mockIntegrationFrameworkConnector
            .submitEclReturn(ArgumentMatchers.eq(eclRegistrationReference), ArgumentMatchers.eq(eclReturnDetails))(
              any()
            )
        )
          .thenReturn(Future.successful(Left(UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR))))

        val result = intercept[UpstreamErrorResponse] {
          await(service.submitEclReturn(eclRegistrationReference, eclReturnDetails, eclReturn))
        }

        result.getMessage() shouldBe "Internal server error"

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }
  }
}
