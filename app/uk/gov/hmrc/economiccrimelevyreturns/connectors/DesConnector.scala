/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.http.HeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyreturns.models.des.ObligationData
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient,
  correlationIdGenerator: CorrelationIdGenerator
)(implicit ec: ExecutionContext) {

  def getObligationData(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[ObligationData] = {
    val desHeaders: Seq[(String, String)] = Seq(
      (HeaderNames.AUTHORIZATION, appConfig.desBearerToken),
      (CustomHeaderNames.Environment, appConfig.desEnvironment),
      (CustomHeaderNames.CorrelationId, correlationIdGenerator.generateCorrelationId)
    )

    httpClient.GET[ObligationData](
      s"${appConfig.desUrl}/enterprise/obligation-data/zecl/$eclRegistrationReference/ECL",
      headers = desHeaders
    )
  }

}
