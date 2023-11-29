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
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError

class SessionISpec extends ISpecBase {

  s"PUT ${routes.SessionController.upsert.url}"                              should {
    "create or update a registration additional info and return 200 OK with the registration" in {
      stubAuthorised()

      val registrationAdditionalInfo = random[RegistrationAdditionalInfo]

      lazy val putResult = callRoute(
        FakeRequest(routes.RegistrationAdditionalInfoController.upsert)
          .withJsonBody(Json.toJson(registrationAdditionalInfo))
      )

      lazy val getResult =
        callRoute(FakeRequest(routes.RegistrationAdditionalInfoController.get(registrationAdditionalInfo.internalId)))

      status(putResult)        shouldBe OK
      status(getResult)        shouldBe OK
      contentAsJson(getResult) shouldBe Json.toJson(registrationAdditionalInfo.copy(lastUpdated = Some(now)))
    }
  }

  s"GET ${routes.RegistrationAdditionalInfoController.get(":id").url}"       should {
    "return 200 OK with registration additional info that is already in the database" in {
      stubAuthorised()

      val registrationAdditionalInfo = random[RegistrationAdditionalInfo]

      callRoute(
        FakeRequest(routes.RegistrationAdditionalInfoController.upsert).withJsonBody(
          Json.toJson(registrationAdditionalInfo)
        )
      ).futureValue

      lazy val result =
        callRoute(FakeRequest(routes.RegistrationAdditionalInfoController.get(registrationAdditionalInfo.internalId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(registrationAdditionalInfo.copy(lastUpdated = Some(now)))
    }

    "return 404 NOT_FOUND when trying to get a registration that doesn't exist" in {
      stubAuthorised()

      val result =
        callRoute(FakeRequest(routes.RegistrationAdditionalInfoController.get("invalidId")))

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError("Unable to find record with id: invalidId")
      )
    }
  }

  s"DELETE ${routes.RegistrationAdditionalInfoController.delete(":id").url}" should {
    "delete a registration and return 200 OK" in {
      stubAuthorised()

      val registrationAdditionalInfo = random[RegistrationAdditionalInfo]

      callRoute(
        FakeRequest(routes.RegistrationAdditionalInfoController.upsert).withJsonBody(
          Json.toJson(registrationAdditionalInfo)
        )
      ).futureValue

      lazy val getResultBeforeDelete =
        callRoute(FakeRequest(routes.RegistrationAdditionalInfoController.get(registrationAdditionalInfo.internalId)))

      lazy val deleteResult =
        callRoute(
          FakeRequest(routes.RegistrationAdditionalInfoController.delete(registrationAdditionalInfo.internalId))
        )

      lazy val getResultAfterDelete =
        callRoute(FakeRequest(routes.RegistrationAdditionalInfoController.get(registrationAdditionalInfo.internalId)))

      status(getResultBeforeDelete) shouldBe OK
      status(deleteResult)          shouldBe NO_CONTENT
      status(getResultAfterDelete)  shouldBe NOT_FOUND
    }
  }

}
