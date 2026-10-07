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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.connectors.traits.{BaseConnector, HipConnector, ReturnsConnector}
import uk.gov.hmrc.economiccrimelevyreturns.models.hip.{HipSubmitReturn, HipSubmitReturnSuccessResponse, HipSuccessWrapper}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, GetEclReturnSubmissionResponse, SubmitEclReturnResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipReturnsConnector @Inject() (
  override val appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries
    with BaseConnector
    with HipConnector
    with ReturnsConnector {

  def getEclReturnSubmission(periodKey: String, eclRegistrationReference: String)(implicit
    hc: HeaderCarrier
  ): Future[GetEclReturnSubmissionResponse] = {

    val correlationId = UUID.randomUUID().toString

    logger.info(s"Attempting to send ECL GET Return Request with correlationid: $correlationId to HIP")
    retryFor[GetEclReturnSubmissionResponse]("HIP Platform - Get ECL return")(retryCondition) {
      executeHip[HipSuccessWrapper[GetEclReturnSubmissionResponse]](
        httpClient
          .get(
            url"${appConfig.hipBaseUrl}/etmp/RESTAdapter/economic-crime-levy/return/$periodKey/$eclRegistrationReference"
          )
          .setHeader(hipHeaders(correlationId): _*),
        s"HIP get ECL return with correlationid: $correlationId"
      ).map(_.success)
    }
  }

  def submitEclReturn(eclRegistrationReference: String, eclReturnSubmission: EclReturnSubmission)(implicit
    hc: HeaderCarrier
  ): Future[SubmitEclReturnResponse] = {

    val correlationId = UUID.randomUUID().toString

    logger.info(s"Attempting to send ECL POST Return Request with correlationid: $correlationId to HIP")
    retryFor[SubmitEclReturnResponse]("HIP Platform - Submit ECL return")(retryCondition) {
      executeHip[HipSubmitReturnSuccessResponse](
        httpClient
          .post(url"${appConfig.hipBaseUrl}/etmp/RESTAdapter/economic-crime-levy/return/$eclRegistrationReference")
          .setHeader(hipHeaders(correlationId): _*)
          .withBody(Json.toJson(eclReturnSubmission)),
        s"HIP submit ECL return with correlationid: $correlationId"
      ).map(_.success.toIfResponse)
    }
  }
}
