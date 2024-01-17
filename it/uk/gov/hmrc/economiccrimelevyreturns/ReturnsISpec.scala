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

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError

class ReturnsISpec extends ISpecBase {

  s"PUT ${routes.ReturnsController.upsertReturn.url}"           should {
    "create or update a return and return 200 OK with the return" in {
      stubAuthorised()

      lazy val putResult = callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(Json.toJson(emptyReturn))
      )

      lazy val getResult =
        callRoute(FakeRequest(routes.ReturnsController.getReturn(emptyReturn.internalId)))

      status(putResult) shouldBe NO_CONTENT
      status(getResult) shouldBe OK
//      contentAsJson(getResult) shouldBe Json.toJson(emptyReturn.copy(lastUpdated = Some(now)))
    }
  }

  s"GET ${routes.ReturnsController.getReturn(":id").url}"       should {
    "return 200 OK with a return that is already in the database" in {
      stubAuthorised()

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(Json.toJson(emptyReturn))
      ).futureValue

      lazy val result =
        callRoute(FakeRequest(routes.ReturnsController.getReturn(emptyReturn.internalId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyReturn.copy(lastUpdated = Some(now)))
    }

    "return 404 NOT_FOUND when trying to get a return that doesn't exist" in {
      stubAuthorised()

      val result = callRoute(FakeRequest(routes.ReturnsController.getReturn(emptyReturn.internalId)))

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${emptyReturn.internalId}")
      )
    }
  }

  s"DELETE ${routes.ReturnsController.deleteReturn(":id").url}" should {
    "delete a return and return 204 NO_CONTENT" in {
      stubAuthorised()

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(Json.toJson(emptyReturn))
      ).futureValue

      lazy val getResultBeforeDelete =
        callRoute(FakeRequest(routes.ReturnsController.getReturn(emptyReturn.internalId)))

      lazy val deleteResult =
        callRoute(FakeRequest(routes.ReturnsController.deleteReturn(emptyReturn.internalId)))

      lazy val getResultAfterDelete =
        callRoute(FakeRequest(routes.ReturnsController.getReturn(emptyReturn.internalId)))

      status(getResultBeforeDelete) shouldBe OK
      status(deleteResult)          shouldBe NO_CONTENT
      status(getResultAfterDelete)  shouldBe NOT_FOUND
    }
  }

}
