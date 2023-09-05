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

import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.utils.PdfGenerator.swap

class PDFConverterSpec extends SpecBase {
  "swap" should {
    "swap single letters in string" in {
      val string   = "ABCDE"
      val expected = "EBCDA"

      val actual = swap(string, 'A', 'E')
      actual shouldBe expected
    }

    "swap all letters in string" in {
      val string   = "qqqqqqqqqq"
      val expected = "wwwwwwwwww"

      val actual = swap(string, 'q', 'w')
      actual shouldBe expected
    }

    "swap some letters in string" in {
      val string   = "qqCqqqCqCq"
      val expected = "wwCwwwCwCw"

      val actual = swap(string, 'q', 'w')
      actual shouldBe expected
    }
  }
}
