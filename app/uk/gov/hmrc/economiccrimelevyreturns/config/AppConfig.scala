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

package uk.gov.hmrc.economiccrimelevyreturns.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val appName: String    = configuration.get[String]("appName")
  val appBaseUrl: String = servicesConfig.baseUrl("self")

  val firstTimeReturnNotableEvent: String =
    configuration.get[String]("microservice.services.nrs.notable-events.first-time-return")
  val amendReturnNotableEvent: String     =
    configuration.get[String]("microservice.services.nrs.notable-events.amend-return")

  val mongoTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val integrationFrameworkUrl: String = servicesConfig.baseUrl("integration-framework")

  val integrationFrameworkBearerToken: String =
    configuration.get[String]("microservice.services.integration-framework.bearerToken")

  val integrationFrameworkEnvironment: String =
    configuration.get[String]("microservice.services.integration-framework.environment")

  val nrsBaseUrl: String = servicesConfig.baseUrl("nrs")

  val nrsApiKey: String = configuration.get[String]("microservice.services.nrs.apiKey")

  val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  val internalAuthBaseUrl: String = servicesConfig.baseUrl("internal-auth")

  val dmsBaseUrl: String = servicesConfig.baseUrl("dms-submission")

  val dmsSubmissionBusinessArea: String =
    configuration.get[String]("microservice.services.dms-submission.amend-returns-submission.businessArea")

  val dmsSubmissionCallbackEndpoint: String =
    configuration.get[String](
      "microservice.services.dms-submission.amend-returns-submission.callbackEndpoint"
    )

  val dmsSubmissionCallbackUrl: String        = s"$appBaseUrl/$appName/$dmsSubmissionCallbackEndpoint"
  val dmsSubmissionClassificationType: String =
    configuration.get[String]("microservice.services.dms-submission.amend-returns-submission.classificationType")
  val dmsSubmissionCustomerId: String         =
    configuration.get[String]("microservice.services.dms-submission.amend-returns-submission.customerId")
  val dmsSubmissionFormId: String             =
    configuration.get[String]("microservice.services.dms-submission.amend-returns-submission.formId")
  val dmsSubmissionSource: String             =
    configuration.get[String]("microservice.services.dms-submission.amend-returns-submission.source")
  val dmsSubmissionUrl: String                = dmsBaseUrl + "/dms-submission/submit"

  val amendReturnsNrsEnabled: Boolean = configuration.get[Boolean]("features.amendReturnsNrsEnabled")

  val integrationFrameworkGetReturnSubmissisonBearerToken: String =
    configuration.get[String]("microservice.services.integration-framework.getReturnSubmissionBearerToken")
}
