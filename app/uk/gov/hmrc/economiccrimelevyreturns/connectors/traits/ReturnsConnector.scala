/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.connectors.traits

import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, GetEclReturnSubmissionResponse, SubmitEclReturnResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait ReturnsConnector {

  def getEclReturnSubmission(periodKey: String, eclRegistrationReference: String)(implicit
    hc: HeaderCarrier
  ): Future[GetEclReturnSubmissionResponse]

  def submitEclReturn(eclRegistrationReference: String, eclReturnSubmission: EclReturnSubmission)(implicit
    hc: HeaderCarrier
  ): Future[SubmitEclReturnResponse]
}
