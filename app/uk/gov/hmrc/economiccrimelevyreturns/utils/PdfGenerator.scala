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

import java.io.ByteArrayOutputStream
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder

object PdfGenerator {
  def buildPdf(html: String): ByteArrayOutputStream = {
    val os       = new ByteArrayOutputStream()
    val builder  = new PdfRendererBuilder
    val renderer = builder
      .useFont(() => getClass.getResourceAsStream("/pdf/arial.ttf"), "Arial")
      .usePdfUaAccessbility(true)
      .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U)
      .withHtmlContent(swap(html, '£', '#'), null)
      .withProducer("HMRC")
      .useFastMode
      .toStream(os)
      .buildPdfRenderer()
    renderer.createPDF()
    renderer.close()

    os
  }

  def swap(text: String, a: Char, b: Char): String = {
    def swapOne(chars: List[Char], a: Char, b: Char): List[Char] =
      if (chars.isEmpty) {
        List()
      } else {
        val c = chars.head
        val n = if (c == a) b else if (c == b) a else c
        List(n) ++ swapOne(chars.tail, a, b)
      }

    val chars = text.toList
    swapOne(chars, a, b).mkString
  }
}
