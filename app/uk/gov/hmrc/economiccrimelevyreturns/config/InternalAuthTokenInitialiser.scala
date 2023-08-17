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

import play.api.Logging
import play.api.http.Status.{CREATED, NO_CONTENT}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.models.Done
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() () extends InternalAuthTokenInitialiser {
  override val initialised: Future[Done] = Future.successful(Done)
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends InternalAuthTokenInitialiser
    with Logging {

  override val initialised: Future[Done] = {
    val authToken = ensureAuthToken()
    val resources = ensureResourceDefinitions()
    val grants    = ensureGrants()
    val seq       = Seq(authToken, resources, grants)

    Future
      .sequence(seq)
      .map(_ => Done)
  }

  Await.result(initialised, 30.seconds)

  private def ensureAuthToken(): Future[Done] =
    authTokenIsValid.flatMap { isValid =>
      if (isValid) {
        logger.info("Auth token is already valid")
        Future.successful(Done)
      } else {
        createClientAuthToken()
      }
    }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("Initialising auth token")
    httpClient
      .post(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.parse(s"""
             |{
             | "token": "${appConfig.internalAuthToken}",
             | "principal": "${appConfig.appName}",
             | "permissions": [
             |   {
             |    "resourceType": "dms-submission",
             |    "resourceLocation": "submit",
             |    "actions": ["WRITE"]
             |   }
             | ]
             |}
          |""".stripMargin)
      )
      .execute
      .flatMap { response =>
        if (response.status == CREATED) {
          logger.info("Auth token initialised")
          Future.successful(Done)
        } else {
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }

  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("Checking auth token")
    httpClient
      .get(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> appConfig.internalAuthToken)
      .execute
      .map(_.status == 200)
  }

  private def ensureGrants(): Future[Done] = {
    logger.info("Creating grants")
    httpClient
      .put(url"${appConfig.internalAuthBaseUrl}/test-only/grants")(HeaderCarrier())
      .withBody(
        Json.parse(
          s"""
             |[
             |  {
             |    "grantees": [
             |      {
             |        "granteeType": "user",
             |        "identifier": "dms-submission"
             |      }
             |    ],
             |    "permissions": [
             |      {
             |        "resourceType": "object-store",
             |        "resourceLocation": "dms-submission",
             |        "actions": [
             |          "READ",
             |          "WRITE",
             |          "DELETE"
             |        ]
             |      }
             |    ]
             |  },
             |  {
             |    "grantees": [
             |      {
             |        "granteeType": "user",
             |        "identifier": "economic-crime-levy-returns"
             |      },
             |      {
             |        "granteeType": "user",
             |        "identifier": "economic-crime-levy-registration"
             |      }
             |    ],
             |    "permissions": [
             |      {
             |        "resourceType": "dms-submission",
             |        "resourceLocation": "submit",
             |        "actions": [
             |          "WRITE"
             |        ]
             |      }
             |    ]
             |  },
             |  {
             |    "grantees": [
             |      {
             |        "granteeType": "user",
             |        "identifier": "dms-submission"
             |      }
             |    ],
             |    "permissions": [
             |      {
             |        "resourceType": "economic-crime-levy-returns",
             |        "resourceLocation": "dms-returns-callback",
             |        "actions": [
             |          "WRITE"
             |        ]
             |      },
             |      {
             |        "resourceType": "economic-crime-levy-registration",
             |        "resourceLocation": "dms-registration-callback",
             |        "actions": [
             |          "WRITE"
             |        ]
             |      }
             |    ]
             |  }
             |]
             |""".stripMargin
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          logger.info("Grants created")
          Future.successful(Done)
        } else {
          Future.failed(new RuntimeException("Unable to create grants"))
        }
      }
  }

  private def ensureResourceDefinitions(): Future[Done] = {
    logger.info("Creating resource definitions")
    httpClient
      .put(url"${appConfig.internalAuthBaseUrl}/test-only/resource-definitions")(HeaderCarrier())
      .withBody(
        Json.parse(
          s"""
             |[
             |  {
             |    "resourceType": "dms-submission",
             |    "actions": [
             |      "READ",
             |      "WRITE",
             |      "DELETE"
             |    ]
             |  },
             |  {
             |    "resourceType": "economic-crime-levy-returns",
             |    "actions": [
             |      "WRITE"
             |    ]
             |  },
             |  {
             |    "resourceType": "economic-crime-levy-registration",
             |    "actions": [
             |      "WRITE"
             |    ]
             |  }
             |]
             |""".stripMargin
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          logger.info("Resource definitions created")
          Future.successful(Done)
        } else {
          Future.failed(new RuntimeException("Unable to create resource definitions"))
        }
      }
  }
}
