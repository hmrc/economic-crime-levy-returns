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

import uk.gov.hmrc.economiccrimelevyreturns.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.RequestStatus.{Failed, Success}
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.{ReturnResult, ReturnSubmittedAuditEvent}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnService @Inject() (
  integrationFrameworkConnector: IntegrationFrameworkConnector,
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  def submitEclReturn(eclRegistrationReference: String, eclReturnSubmission: EclReturnSubmission, eclReturn: EclReturn)(
    implicit hc: HeaderCarrier
  ): Future[SubmitEclReturnResponse] =
    integrationFrameworkConnector.submitEclReturn(eclRegistrationReference, eclReturnSubmission).map {
      case Right(submitEclReturnResponse) =>
        auditConnector.sendExtendedEvent(
          ReturnSubmittedAuditEvent(
            eclReturn,
            eclRegistrationReference,
            ReturnResult(Success, submitEclReturnResponse.chargeReference, None),
            eclReturn.returnType
          ).extendedDataEvent
        )

        submitEclReturnResponse
      case Left(e)                        =>
        auditConnector.sendExtendedEvent(
          ReturnSubmittedAuditEvent(
            eclReturn,
            eclRegistrationReference,
            ReturnResult(Failed, None, Some(e.getMessage())),
            eclReturn.returnType
          ).extendedDataEvent
        )
        throw e
    }

}
