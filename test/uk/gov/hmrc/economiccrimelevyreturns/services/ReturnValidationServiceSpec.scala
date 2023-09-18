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

import cats.implicits.catsSyntaxValidatedId
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Gen
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.Band._
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataValidationError, DataValidationErrorList}
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.utils.SchemaValidator

import java.time.{Clock, Instant, ZoneId}

class ReturnValidationServiceSpec extends SpecBase {

  private val fixedPointInTime             = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock             = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)
  val mockSchemaValidator: SchemaValidator = mock[SchemaValidator]
  val service                              = new ReturnValidationService(stubClock, mockSchemaValidator)

  "validateReturn" should {
    "return the ECL return details if the ECL return is valid" in forAll { validEclReturn: ValidEclReturn =>
      when(
        mockSchemaValidator.validateAgainstJsonSchema(
          ArgumentMatchers.eq(validEclReturn.expectedEclReturnSubmission),
          any()
        )(any())
      ).thenReturn(validEclReturn.expectedEclReturnSubmission.validNel)

      val result = await(service.validateReturn(validEclReturn.eclReturn).value)

      result shouldBe Right(validEclReturn.expectedEclReturnSubmission)
    }

    "return DataValidationErrorList containing the list of errors when unconditional mandatory ECL return data items are missing" in {
      val eclReturn = EclReturn.empty("internalId")

      val expectedErrors = List(
        DataValidationError.DataMissing("Relevant AP 12 months choice is missing"),
        DataValidationError.DataMissing("Relevant AP revenue is missing"),
        DataValidationError.DataMissing("Calculated liability is missing"),
        DataValidationError.DataMissing("Contact name is missing"),
        DataValidationError.DataMissing("Contact role is missing"),
        DataValidationError.DataMissing("Contact email address is missing"),
        DataValidationError.DataMissing("Contact telephone number is missing"),
        DataValidationError.DataMissing("Obligation details is missing")
      )

      val result = await(service.validateReturn(eclReturn).value)

      result shouldBe Left(DataValidationErrorList(expectedErrors))
    }

    "return an error if the relevant AP is not 12 months and the relevant AP length is missing" in forAll {
      validEclReturn: ValidEclReturn =>
        val invalidEclReturn =
          validEclReturn.eclReturn.copy(relevantAp12Months = Some(false), relevantApLength = None)

        val result = await(service.validateReturn(invalidEclReturn).value)

        result shouldBe Left(
          DataValidationErrorList(
            List(
              DataValidationError.DataMissing(
                "Relevant AP length is missing"
              )
            )
          )
        )
    }

    "return an error if the calculated band size is not Small and the AML regulated activity carried out for the full financial year is missing" in forAll(
      arbValidEclReturn.arbitrary,
      Gen.oneOf[Band](Medium, Large, VeryLarge)
    ) { (validEclReturn: ValidEclReturn, calculatedBand: Band) =>
      val calculatedLiability = validEclReturn.eclReturn.calculatedLiability.get.copy(calculatedBand = calculatedBand)
      val invalidEclReturn    =
        validEclReturn.eclReturn
          .copy(calculatedLiability = Some(calculatedLiability), carriedOutAmlRegulatedActivityForFullFy = None)

      val result = await(service.validateReturn(invalidEclReturn).value)

      result shouldBe Left(
        DataValidationErrorList(
          List(
            DataValidationError.DataMissing(
              "Carried out AML regulated activity for full FY choice is missing"
            )
          )
        )
      )
    }

    "return an error if AML regulated activity was not carried out for the full financial year and the AML regulated activity length is missing" in forAll {
      validEclReturn: ValidEclReturn =>
        val invalidEclReturn =
          validEclReturn.eclReturn
            .copy(carriedOutAmlRegulatedActivityForFullFy = Some(false), amlRegulatedActivityLength = None)

        val result = await(service.validateReturn(invalidEclReturn).value)

        result shouldBe Left(
          DataValidationErrorList(
            List(
              DataValidationError.DataMissing(
                "AML regulated activity length is missing"
              )
            )
          )
        )
    }
  }
}
