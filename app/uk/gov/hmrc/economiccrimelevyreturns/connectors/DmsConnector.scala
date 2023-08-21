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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import uk.gov.hmrc.http.HttpReads.Implicits._
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Retries, UpstreamErrorResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries
    with Logging {

  private def retryCondition: PartialFunction[Exception, Boolean] = {
    case e: UpstreamErrorResponse if UpstreamErrorResponse.Upstream5xxResponse.unapply(e).isDefined => true
  }

  def sendPdf(
    body: Source[MultipartFormData.Part[Source[ByteString, NotUsed]] with Product with Serializable, NotUsed]
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    retryFor[Either[UpstreamErrorResponse, HttpResponse]]("DMS submission")(retryCondition) {
      httpClient
        .post(new URL(appConfig.dmsSubmissionUrl))
        .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
        .withBody(body)
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .map {
          case Right(httpResponse: HttpResponse)          => Right(httpResponse)
          case Left(errorResponse: UpstreamErrorResponse) => Left(errorResponse)
        }
    }
}
