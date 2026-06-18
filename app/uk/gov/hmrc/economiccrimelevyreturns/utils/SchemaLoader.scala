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

import scala.io.Source
import com.eclipsesource.schema.*
import com.eclipsesource.schema.drafts.Version7
import com.eclipsesource.schema.drafts.Version7.*
import play.api.libs.json.Json

object SchemaLoader {

  def loadSchema(schemaFileName: String): SchemaType = {
    val schemaFilePath = s"/schemas/$schemaFileName"
    val resource       = getClass.getResourceAsStream(schemaFilePath)
    val source         = Source.fromInputStream(resource)

    val jsonSchema =
      try source.getLines().mkString
      finally source.close()

    Json.fromJson[SchemaType](Json.parse(jsonSchema)).get
  }

}
