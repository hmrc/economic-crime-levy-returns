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

import cats.data.Validated.Valid
import io.circe.schema.Schema
import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationError

class SchemaValidatorSpec extends SpecBase {
  case class TestObject(foo: String, bar: String)

  object TestObject {
    implicit val format: OFormat[TestObject] = Json.format[TestObject]
  }

  private val testJsonSchema: Schema = Schema
    .loadFromString("""{
        |  "$schema": "http://json-schema.org/draft-07/schema#",
        |  "title": "TestObject",
        |  "type": "object",
        |  "properties": {
        |    "foo": {
        |      "type": "string",
        |      "description": "A string value"
        |    },
        |    "bar": {
        |      "type": "string",
        |      "description": "A string value",
        |      "pattern": "^[a-z]*$"
        |    }
        |  }
        |}""".stripMargin)
    .getOrElse(fail("failed to create test JSON schema"))

  val schemaValidator: SchemaValidator = new SchemaValidator

  "validateAgainstJsonSchema" should {
    "serialize an object into JSON and validate it against a JSON schema, returning the given validated object if there are no errors" in {
      val result = schemaValidator.validateAgainstJsonSchema(TestObject("foo", "bar"), testJsonSchema)

      result shouldBe Valid(TestObject("foo", "bar"))
    }

    "serialize an object into JSON and validate it against a JSON schema, returning errors if there are any" in {
      val result = schemaValidator.validateAgainstJsonSchema(TestObject("foo", "123"), testJsonSchema)

      result.isValid shouldBe false
      result.leftMap(nec =>
        nec.toList shouldEqual
          List(DataValidationError.SchemaValidationError("Schema validation error for field: #/bar (pattern)"))
      )
    }
  }
}
