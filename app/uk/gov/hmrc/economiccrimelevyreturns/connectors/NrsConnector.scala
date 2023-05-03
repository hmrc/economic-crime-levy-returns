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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.{NrsSubmission, NrsSubmissionResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Retries, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends Retries {

  private val nrsSubmissionUrl: String = s"${appConfig.nrsBaseUrl}/submission"

  private def nrsHeaders: Seq[(String, String)] = Seq(
    (HeaderNames.CONTENT_TYPE, MimeTypes.JSON),
    (CustomHeaderNames.ApiKey, appConfig.nrsApiKey)
  )

  private def retryCondition: PartialFunction[Exception, Boolean] = {
    case e: UpstreamErrorResponse if UpstreamErrorResponse.Upstream5xxResponse.unapply(e).isDefined => true
  }

  def submitToNrs(nrsSubmission: NrsSubmission)(implicit
    hc: HeaderCarrier
  ): Future[NrsSubmissionResponse] =
    retryFor[NrsSubmissionResponse]("NRS submission")(retryCondition)(
      httpClient.POST[NrsSubmission, NrsSubmissionResponse](nrsSubmissionUrl, nrsSubmission, headers = nrsHeaders)
    )

}
