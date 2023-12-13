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
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper.HEADER_X_CORRELATION_ID
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries
    with BaseConnector {

  private def integrationFrameworkHeaders(correlationId: String): Seq[(String, String)] = Seq(
    (HeaderNames.AUTHORIZATION, s"Bearer ${appConfig.integrationFrameworkBearerToken}"),
    (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
    (CustomHeaderNames.CorrelationId, correlationId)
  )

  def submitEclReturn(eclRegistrationReference: String, eclReturnSubmission: EclReturnSubmission)(implicit
    hc: HeaderCarrier
  ): Future[SubmitEclReturnResponse] = {
    val correlationId = hc.headers(scala.Seq(HEADER_X_CORRELATION_ID)) match {
      case Nil          =>
        UUID.randomUUID().toString
      case Seq((_, id)) =>
        id
    }

    retryFor[SubmitEclReturnResponse]("Integration framework - ECL return")(retryCondition) {
      httpClient
        .post(url"${appConfig.integrationFrameworkUrl}/economic-crime-levy/return/$eclRegistrationReference")
        .setHeader(integrationFrameworkHeaders(correlationId): _*)
        .withBody(Json.toJson(eclReturnSubmission))
        .executeAndDeserialise[SubmitEclReturnResponse]
    }
  }
}
