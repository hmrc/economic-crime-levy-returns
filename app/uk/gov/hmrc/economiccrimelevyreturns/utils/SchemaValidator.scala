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
import com.eclipsesource.schema.*
import com.eclipsesource.schema.drafts.Version7
import com.eclipsesource.schema.drafts.Version7.*
import io.circe.parser.{parse => circeParse}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, OFormat}
import io.circe.{Json => circeJson}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError

import javax.inject.Inject

class JsonSchemaValidator @Inject() () {

  private val validator: SchemaValidator = SchemaValidator(Some(Version7))

  def validateAgainstJsonSchema[T](validationObject: T, schema: SchemaType)(implicit
    format: OFormat[T]
  ): Either[DataValidationError, Unit] = {

    val json: JsValue = Json.toJson(validationObject)

    validator.validate(schema, json) match {
      case JsSuccess(_, _) => Right(())
      case JsError(errors) =>
        Left(
          DataValidationError.SchemaValidationError(errorMessage =
            s"Schema validation error: ${Json.stringify(errors.toJson)}"
          )
        )
    }

  }

}
