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

package uk.gov.hmrc.economiccrimelevyreturns.utils

import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.util.UUID

object CorrelationIdGenerator {

  val HEADER_X_CORRELATION_ID: String = "X-Correlation-Id"

  def headerCarrierWithCorrelationId(request: Request[_]): HeaderCarrier = {
    val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    hcFromRequest
      .headers(scala.Seq(HEADER_X_CORRELATION_ID)) match {
      case Nil =>
        hcFromRequest.withExtraHeaders((HEADER_X_CORRELATION_ID, UUID.randomUUID().toString))
      case _   =>
        hcFromRequest
    }
  }
}
