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
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError.DataMissing
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataValidationError, DataValidationErrors}

class ReturnValidationISpec extends ISpecBase {

  s"GET ${routes.ReturnValidationController.getValidationErrors(":id").url}" should {
    "return 204 NO_CONTENT when the ECL return data is valid" in {
      stubAuthorised()

      val validReturn = random[ValidEclReturn]

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(
          Json.toJson(validReturn.eclReturn)
        )
      ).futureValue

      lazy val validationResult =
        callRoute(
          FakeRequest(
            routes.ReturnValidationController.getValidationErrors(validReturn.eclReturn.internalId)
          )
        )

      status(validationResult) shouldBe NO_CONTENT
    }

    "return 200 OK with validation errors in the JSON response body when the ECL return data is invalid" in {
      stubAuthorised()

      val internalId = random[String]

      val invalidReturn = EclReturn.empty(internalId)

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(Json.toJson(invalidReturn))
      ).futureValue

      lazy val validationResult =
        callRoute(FakeRequest(routes.ReturnValidationController.getValidationErrors(internalId)))

      val expectedErrors = Seq(
        DataValidationError(DataMissing, "Relevant AP 12 months choice is missing"),
        DataValidationError(DataMissing, "Relevant AP revenue is missing"),
        DataValidationError(DataMissing, "Calculated liability is missing"),
        DataValidationError(DataMissing, "Contact name is missing"),
        DataValidationError(DataMissing, "Contact role is missing"),
        DataValidationError(DataMissing, "Contact email address is missing"),
        DataValidationError(DataMissing, "Contact telephone number is missing")
      )

      status(validationResult)                                      shouldBe OK
      contentAsJson(validationResult).as[DataValidationErrors].errors should contain allElementsOf expectedErrors
    }

    "return 404 NOT_FOUND when there is no ECL return data to validate" in {
      stubAuthorised()

      val internalId = random[String]

      lazy val validationResult =
        callRoute(FakeRequest(routes.ReturnValidationController.getValidationErrors(internalId)))

      status(validationResult) shouldBe NOT_FOUND
    }
  }

}
