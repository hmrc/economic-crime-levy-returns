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

package uk.gov.hmrc.economiccrimelevyreturns.models.dms

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class SubmissionItemStatus extends EnumEntry

object SubmissionItemStatus extends Enum[SubmissionItemStatus] with PlayJsonEnum[SubmissionItemStatus] {

  case object Submitted extends SubmissionItemStatus
  case object Forwarded extends SubmissionItemStatus
  case object Processed extends SubmissionItemStatus
  case object Failed extends SubmissionItemStatus
  case object Completed extends SubmissionItemStatus

  override def values: IndexedSeq[SubmissionItemStatus] = findValues
}
