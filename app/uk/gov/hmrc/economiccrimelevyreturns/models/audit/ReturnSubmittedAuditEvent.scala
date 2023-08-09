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

package uk.gov.hmrc.economiccrimelevyreturns.models.audit

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyreturns.models.{EclReturn, ReturnType}

sealed trait RequestStatus

object RequestStatus {
  case object Success extends RequestStatus
  case object Failed extends RequestStatus

  implicit val writes: Writes[RequestStatus] = (o: RequestStatus) => JsString(o.toString)
}

case class ReturnResult(status: RequestStatus, chargeReference: Option[String], failureReason: Option[String])

object ReturnResult {
  implicit val writes: OWrites[ReturnResult] = Json.writes[ReturnResult]
}

case class ReturnSubmittedAuditEvent(
  returnData: EclReturn,
  eclReference: String,
  submissionResult: ReturnResult,
  returnType: Option[ReturnType]
) extends AuditEvent {
  override val auditType: String   = "ReturnSubmitted"
  override val detailJson: JsValue = Json.toJson(this)
}

object ReturnSubmittedAuditEvent {
  implicit val writes: OWrites[ReturnSubmittedAuditEvent] = Json.writes[ReturnSubmittedAuditEvent]
}
