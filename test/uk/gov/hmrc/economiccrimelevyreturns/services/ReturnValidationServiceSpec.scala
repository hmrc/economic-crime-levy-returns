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

package uk.gov.hmrc.economiccrimelevyreturns.services

import cats.data.Validated.Valid
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError.DataMissing

class ReturnValidationServiceSpec extends SpecBase {

  val service = new ReturnValidationService

  "validateReturn" should {
    "return the ECL return if the return is valid" in forAll { validEclReturn: ValidEclReturn =>
      val result = service.validateReturn(validEclReturn.eclReturn)

      result shouldBe Valid(validEclReturn.eclReturn)
    }

    "return a non-empty list of errors when unconditional mandatory ECL return data items are missing" in {
      val eclReturn = EclReturn.empty("internalId")

      val expectedErrors = Seq(
        DataValidationError(DataMissing, "Relevant AP 12 months choice is missing"),
        DataValidationError(DataMissing, "Relevant AP revenue is missing"),
        DataValidationError(DataMissing, "Carried out AML regulated activity for full FY choice is missing"),
        DataValidationError(DataMissing, "Calculated liability is missing")
      )

      val result = service.validateReturn(eclReturn)

      result.isValid shouldBe false
      result.leftMap(nel => nel.toList should contain theSameElementsAs expectedErrors)
    }

    "return an error if the relevant AP is not 12 months and the relevant AP length is missing" in forAll {
      validEclReturn: ValidEclReturn =>
        val invalidEclReturn =
          validEclReturn.eclReturn.copy(relevantAp12Months = Some(false), relevantApLength = None)

        val result = service.validateReturn(invalidEclReturn)

        result.isValid shouldBe false
        result.leftMap(nel =>
          nel.toList should contain only DataValidationError(
            DataMissing,
            "Relevant AP length is missing"
          )
        )
    }

    "return an error if AML regulated activity was not carried out for the full financial year and the AML regulated activity length is missing" in forAll {
      validEclReturn: ValidEclReturn =>
        val invalidEclReturn =
          validEclReturn.eclReturn
            .copy(carriedOutAmlRegulatedActivityForFullFy = Some(false), amlRegulatedActivityLength = None)

        val result = service.validateReturn(invalidEclReturn)

        result.isValid shouldBe false
        result.leftMap(nel =>
          nel.toList should contain only DataValidationError(
            DataMissing,
            "AML regulated activity length is missing"
          )
        )
    }
  }
}
