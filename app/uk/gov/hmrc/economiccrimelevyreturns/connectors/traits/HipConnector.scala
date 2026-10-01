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

import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

trait HipConnector extends Logging {
  
  val appConfig: AppConfig
  
  def hipHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      HeaderNames.ACCEPT -> MimeTypes.JSON,
      "correlationid" -> correlationId,
      "X-Originating-System" -> "ECL",
      "X-Receipt-Date" -> DateTimeFormatter.ISO_INSTANT.format(
        Instant.now()
          .truncatedTo(ChronoUnit.SECONDS)
      ),
      "X-Transmitting-System" -> "HIP",
      "Authorization" -> s"Basic ${appConfig.hipAuthorizationToken}"
    )
}
