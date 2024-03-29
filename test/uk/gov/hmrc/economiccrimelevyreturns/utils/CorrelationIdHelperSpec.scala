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

package uk.gov.hmrc.economiccrimelevyreturns.utils

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames

import java.util.UUID

class CorrelationIdHelperSpec extends SpecBase {

  "headerCarrierWithCorrelationId" should {
    "return a correlation id when not present in request" in {

      val correlationIdHeader = CustomHeaderNames.xCorrelationId

      val result = CorrelationIdHelper.headerCarrierWithCorrelationId(fakeRequest)

      val headerKeys: Seq[String] = result.extraHeaders.map(_._1)

      headerKeys should contain atLeastOneElementOf Seq(correlationIdHeader)
    }

    "return existing correlation id when present in request" in {

      val correlationIdHeader = CustomHeaderNames.xCorrelationId
      val correlationId       = UUID.randomUUID().toString

      val requestWithCorrelationId = fakeRequest
        .withHeaders((correlationIdHeader, correlationId))

      val result = CorrelationIdHelper.headerCarrierWithCorrelationId(requestWithCorrelationId)

      result.otherHeaders should contain atLeastOneElementOf Map(correlationIdHeader -> correlationId)
    }
  }
}
