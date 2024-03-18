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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.{NrsSubmission, NrsSubmissionResponse}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends BaseConnector {

  private val nrsSubmissionUrl: String = s"${appConfig.nrsBaseUrl}/submission"

  private def nrsHeaders: Seq[(String, String)] = Seq(
    (HeaderNames.CONTENT_TYPE, MimeTypes.JSON),
    (CustomHeaderNames.apiKey, appConfig.nrsApiKey)
  )

  def submitToNrs(nrsSubmission: NrsSubmission)(implicit
    hc: HeaderCarrier
  ): Future[NrsSubmissionResponse] =
    retryFor[NrsSubmissionResponse]("NRS submission")(retryCondition) {
      httpClient
        .post(url"$nrsSubmissionUrl")
        .setHeader(nrsHeaders: _*)
        .withBody(Json.toJson(nrsSubmission))
        .executeAndDeserialise[NrsSubmissionResponse]
    }
}
