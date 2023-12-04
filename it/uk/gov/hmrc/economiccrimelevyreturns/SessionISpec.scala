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

package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError

class SessionISpec extends ISpecBase {

  s"PUT ${routes.SessionController.upsert.url}"           should {
    "create or update a session data additional info and return 200 OK with the registration" in {
      stubAuthorised()

      val sessionData = random[SessionData]

      lazy val putResult = callRoute(
        FakeRequest(routes.SessionController.upsert)
          .withJsonBody(Json.toJson(sessionData))
      )

      lazy val getResult =
        callRoute(FakeRequest(routes.SessionController.get(sessionData.internalId)))

      status(putResult)        shouldBe OK
      status(getResult)        shouldBe OK
      contentAsJson(getResult) shouldBe Json.toJson(sessionData.copy(lastUpdated = Some(now)))
    }
  }

  s"GET ${routes.SessionController.get(":id").url}"       should {
    "return 200 OK with session data additional info that is already in the database" in {
      stubAuthorised()

      val sessionData = random[SessionData]

      callRoute(
        FakeRequest(routes.SessionController.upsert).withJsonBody(
          Json.toJson(sessionData)
        )
      ).futureValue

      lazy val result =
        callRoute(FakeRequest(routes.SessionController.get(sessionData.internalId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(sessionData.copy(lastUpdated = Some(now)))
    }

    "return 404 NOT_FOUND when trying to get a session data that doesn't exist" in {
      stubAuthorised()

      val result =
        callRoute(FakeRequest(routes.SessionController.get("invalidId")))

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError("Unable to find record with id: invalidId")
      )
    }
  }

  s"DELETE ${routes.SessionController.delete(":id").url}" should {
    "delete a session data and return 200 OK" in {
      stubAuthorised()

      val sessionData = random[SessionData]

      callRoute(
        FakeRequest(routes.SessionController.upsert).withJsonBody(
          Json.toJson(sessionData)
        )
      ).futureValue

      lazy val getResultBeforeDelete =
        callRoute(FakeRequest(routes.SessionController.get(sessionData.internalId)))

      lazy val deleteResult =
        callRoute(
          FakeRequest(routes.SessionController.delete(sessionData.internalId))
        )

      lazy val getResultAfterDelete =
        callRoute(FakeRequest(routes.SessionController.get(sessionData.internalId)))

      status(getResultBeforeDelete) shouldBe OK
      status(deleteResult)          shouldBe OK
      status(getResultAfterDelete)  shouldBe NOT_FOUND
    }
  }

}
