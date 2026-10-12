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

case class HipInner422Err(errorId: String, processingDate: String, text: String)

case object HipInner422Err {
  given format: OFormat[HipInner422Err] = Json.format[HipInner422Err]
}

case class Hip422Error(error: HipInner422Err)

case object Hip422Error {
  given format: OFormat[Hip422Error] = Json.format[Hip422Error]
}
