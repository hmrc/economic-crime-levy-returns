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

package uk.gov.hmrc.economiccrimelevyreturns.services

import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.AuditError
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  "sendReturnSubmittedEvent" should {
    "return unit when audit success" in forAll {
      (eclReturn: EclReturn, eclRegistrationReference: String, chargeReference: String) =>
        reset(mockAuditConnector)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val sut = new AuditService(mockAuditConnector)

        val result: Either[AuditError, Unit] = await(
          sut
            .sendReturnSubmittedEvent(eclReturn, eclRegistrationReference, Some(chargeReference))
            .value
        )

        result shouldBe Right(())
    }

    "return AuditError.BadGateway when audit failed" in forAll {
      (eclReturn: EclReturn, eclRegistrationReference: String, chargeReference: String, errorMessage: String) =>
        reset(mockAuditConnector)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Failure(errorMessage, None)))

        val sut = new AuditService(mockAuditConnector)

        val result: Either[AuditError, Unit] = await(
          sut
            .sendReturnSubmittedEvent(eclReturn, eclRegistrationReference, Some(chargeReference))
            .value
        )

        result shouldBe Left(AuditError.BadGateway(s"Return Submitted Audit Failed - $errorMessage", BAD_GATEWAY))
    }

    "return AuditError.BadGateway when audit disabled" in forAll {
      (eclReturn: EclReturn, eclRegistrationReference: String, chargeReference: String) =>
        reset(mockAuditConnector)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Disabled))

        val sut = new AuditService(mockAuditConnector)

        val result: Either[AuditError, Unit] = await(
          sut
            .sendReturnSubmittedEvent(eclReturn, eclRegistrationReference, Some(chargeReference))
            .value
        )

        result shouldBe Left(AuditError.BadGateway("Audit is disabled for the audit connector", BAD_GATEWAY))
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll {
      (eclReturn: EclReturn, eclRegistrationReference: String, chargeReference: String, errorMessage: String) =>
        reset(mockAuditConnector)

        val throwable = new Exception(errorMessage)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.failed(throwable))

        val sut = new AuditService(mockAuditConnector)

        val result: Either[AuditError, Unit] = await(
          sut
            .sendReturnSubmittedEvent(eclReturn, eclRegistrationReference, Some(chargeReference))
            .value
        )

        result shouldBe Left(AuditError.InternalUnexpectedError(Some(throwable)))
    }
  }
}
