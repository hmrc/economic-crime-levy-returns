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

import play.api.http.Status.BAD_GATEWAY

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.RequestStatus.{Failed, Success}
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.{ReturnResult, ReturnSubmittedAuditEvent}
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.AuditError
import scala.util.control.NonFatal
import cats.data.EitherT

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  def sendReturnSubmittedEvent(
    eclReturn: EclReturn,
    eclRegistrationReference: String,
    chargeReference: Option[String]
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] =
    EitherT {

      val submittedAuditEvent = ReturnSubmittedAuditEvent(
        eclReturn,
        eclRegistrationReference,
        ReturnResult(Success, chargeReference, None)
      ).extendedDataEvent

      auditConnector
        .sendExtendedEvent(submittedAuditEvent)(hc, ec)
        .map {
          case AuditResult.Success            => Right(())
          case AuditResult.Failure(reason, _) => Left(AuditError.BadGateway(reason = reason, code = BAD_GATEWAY))
          case AuditResult.Disabled           =>
            Left(AuditError.BadGateway(reason = "Audit is disabled for the audit connector", code = BAD_GATEWAY))
        }
        .recover { case NonFatal(e) =>
          Left(AuditError.InternalUnexpectedError(e.getMessage, Some(e)))
        }
    }

}
