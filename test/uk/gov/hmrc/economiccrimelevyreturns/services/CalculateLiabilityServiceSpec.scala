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

import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.Band._
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.utils.ApportionmentUtils.yearInDays

class CalculateLiabilityServiceSpec extends SpecBase {

  case class ExpectedBands(
    expectedSmallTo: Long,
    expectedMediumTo: Long,
    expectedLargeTo: Long,
    generatedRevenue: Long,
    expectedBand: Band
  )

  object ExpectedBands {
    def apply(smallTo: Long, mediumTo: Long, largeTo: Long, band: Band)(implicit
      appConfig: AppConfig
    ): ExpectedBands = {
      val revenue = band match {
        case Small     => Gen.chooseNum[Long](appConfig.defaultBands.small.from, smallTo - 1).sample.get
        case Medium    => Gen.chooseNum[Long](smallTo, mediumTo - 1).sample.get
        case Large     => Gen.chooseNum[Long](mediumTo, largeTo - 1).sample.get
        case VeryLarge => Gen.chooseNum[Long](largeTo, appConfig.defaultBands.veryLarge.to).sample.get
      }

      ExpectedBands(smallTo, mediumTo, largeTo, revenue, band)
    }
  }

  val service = new CalculateLiabilityService(appConfig)

  implicit val config: AppConfig = appConfig

  private val sTo      = appConfig.defaultBands.small.to
  private val mTo      = appConfig.defaultBands.medium.to
  private val lTo      = appConfig.defaultBands.large.to
  private val sAmount  = BigDecimal(appConfig.defaultSmallAmount)
  private val mAmount  = BigDecimal(appConfig.defaultMediumAmount)
  private val lAmount  = BigDecimal(appConfig.defaultLargeAmount)
  private val vlAmount = BigDecimal(appConfig.defaultVeryLargeAmount)

  "calculateLiability" should {
    "return the correctly calculated liability based on both the length of the relevant AP and AML regulated activity" in forAll(
      Table(
        (
          "relevantApLength",
          "amlRegulatedActivityLength",
          "expectedBands",
          "expectedAmountDue"
        ),
        (yearInDays, yearInDays, ExpectedBands(sTo, mTo, lTo, Small), sAmount),
        (yearInDays, yearInDays, ExpectedBands(sTo, mTo, lTo, Medium), mAmount),
        (yearInDays, yearInDays, ExpectedBands(sTo, mTo, lTo, Large), lAmount),
        (yearInDays, yearInDays, ExpectedBands(sTo, mTo, lTo, VeryLarge), vlAmount),
        (245, yearInDays, ExpectedBands(6846575L, 24164383L, 671232876L, Small), sAmount),
        (182, yearInDays, ExpectedBands(5086027L, 17950684L, 498630136L, Medium), mAmount),
        (73, yearInDays, ExpectedBands(2040000L, 7200000L, 200000000L, Large), lAmount),
        (450, yearInDays, ExpectedBands(12575342L, 44383561L, 1232876712L, VeryLarge), vlAmount),
        (yearInDays, 120, ExpectedBands(sTo, mTo, lTo, Small), sAmount),
        (yearInDays, 60, ExpectedBands(sTo, mTo, lTo, Medium), BigDecimal(1643.83)),
        (yearInDays, 204, ExpectedBands(sTo, mTo, lTo, Large), BigDecimal(20120.54)),
        (yearInDays, 330, ExpectedBands(sTo, mTo, lTo, VeryLarge), BigDecimal(226027.39)),
        (314, 92, ExpectedBands(8774794L, 30969863L, 860273972L, Small), sAmount),
        (113, 198, ExpectedBands(3157808L, 11145205L, 309589041L, Medium), BigDecimal(5424.65)),
        (284, 300, ExpectedBands(7936438L, 28010958L, 778082191L, Large), BigDecimal(29589.04)),
        (91, 256, ExpectedBands(2543013L, 8975342L, 249315068L, VeryLarge), BigDecimal(175342.46))
      )
    ) {
      (
        relevantApLength: Int,
        amlRegulatedActivityLength: Int,
        expectedBands: ExpectedBands,
        expectedAmountDue: BigDecimal
      ) =>
        val result = service.calculateLiability(
          CalculateLiabilityRequest(
            amlRegulatedActivityLength = amlRegulatedActivityLength,
            relevantApLength = relevantApLength,
            ukRevenue = expectedBands.generatedRevenue
          )
        )

        val expectedSmallBand: BandRange     =
          BandRange(from = appConfig.defaultBands.small.from, to = expectedBands.expectedSmallTo)
        val expectedMediumBand: BandRange    =
          BandRange(from = expectedBands.expectedSmallTo, to = expectedBands.expectedMediumTo)
        val expectedLargeBand: BandRange     =
          BandRange(from = expectedBands.expectedMediumTo, to = expectedBands.expectedLargeTo)
        val expectedVeryLargeBand: BandRange =
          BandRange(from = expectedBands.expectedLargeTo, to = appConfig.defaultBands.veryLarge.to)

        result shouldBe CalculatedLiability(
          amountDue = expectedAmountDue,
          bands = Bands(
            small = expectedSmallBand,
            medium = expectedMediumBand,
            large = expectedLargeBand,
            veryLarge = expectedVeryLargeBand
          ),
          calculatedBand = expectedBands.expectedBand
        )
    }
  }

}
