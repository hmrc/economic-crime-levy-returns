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

package uk.gov.hmrc.economiccrimelevyreturns.utils

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import io.circe.parser.{parse => circeParse}
import io.circe.schema.Schema
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError.SchemaValidationError

import javax.inject.Inject

class SchemaValidator @Inject() () {

  type ValidationResult[A] = ValidatedNel[DataValidationError, A]

  def validateAgainstJsonSchema[T](o: T, schema: Schema)(implicit
    format: OFormat[T]
  ): ValidationResult[T] = {
    val jsonString = Json.toJson(o).toString()
    val json       = circeParse(jsonString).getOrElse(
      throw new Exception("Could not transform play JSON into circe JSON for schema validation")
    )

    schema
      .validate(json) match {
      case Valid(_)   => Valid(o)
      case Invalid(e) =>
        Invalid(
          e.map(e =>
            DataValidationError(
              code = SchemaValidationError,
              message = s"Schema validation error for field: ${e.location} (${Option(e.keyword).getOrElse("unknown")})"
            )
          )
        )
    }
  }

}
