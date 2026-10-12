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

package uk.gov.hmrc.economiccrimelevyreturns.models.hip

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse

import java.time.Instant

final case class HipSubmitReturnSuccessResponse(success: HipSubmitReturn)

object HipSubmitReturnSuccessResponse {
  implicit val format: OFormat[HipSubmitReturnSuccessResponse] = Json.format[HipSubmitReturnSuccessResponse]
}

final case class HipSubmitReturn(
  processingDate: Instant,
  eclReference: String,
  submissionId: Option[String],
  chargeReference: Option[String],
  amount: BigDecimal,
  dueDate: String
) {

  /*
  The IF response only contains the below fields & is also the response of this endpoints call. Furthermore, the
  requirements of this migration doesn't include a change in the API's response so it's safe to drop
  the extra fields above. Play ignores unused fields so SubmitEclReturnResponse could have been used in the HipConnector
  for serialization but this method makes it clearer the fields are being acknowledged & dropped intentionally.
   */
  def toIfResponse: SubmitEclReturnResponse =
    SubmitEclReturnResponse(
      processingDate = processingDate,
      chargeReference = chargeReference
    )
}

object HipSubmitReturn {
  implicit val format: OFormat[HipSubmitReturn] = Json.format[HipSubmitReturn]
}
