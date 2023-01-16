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

import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.Band.{Large, Medium, Small, VeryLarge}
import uk.gov.hmrc.economiccrimelevyreturns.models._
import uk.gov.hmrc.economiccrimelevyreturns.utils.ApportionmentUtils

import javax.inject.Inject
import scala.math.BigDecimal.RoundingMode

class CalculateLiabilityService @Inject() (appConfig: AppConfig) {

  private def calculateBand(relevantApLength: Int, ukRevenue: Long): (Bands, Band) = {
    val smallBand     = appConfig.defaultBands.small.apportion(relevantApLength)
    val mediumBand    = appConfig.defaultBands.medium.apportion(relevantApLength)
    val largeBand     = appConfig.defaultBands.large.apportion(relevantApLength)
    val veryLargeBand =
      appConfig.defaultBands.veryLarge.apportion(relevantApLength).copy(to = appConfig.defaultBands.veryLarge.to)

    val bands: Bands = Bands(
      smallBand,
      mediumBand,
      largeBand,
      veryLargeBand
    )

    val band = ukRevenue match {
      case n if n >= smallBand.from && n < smallBand.to   => Small
      case n if n >= mediumBand.from && n < mediumBand.to => Medium
      case n if n >= largeBand.from && n < largeBand.to   => Large
      case n if n >= veryLargeBand.from                   => VeryLarge
      case _                                              => throw new IllegalStateException(s"Revenue $ukRevenue does not fall into any of the bands")
    }

    (bands, band)
  }

  private def calculateAmountDue(band: Band, amlRegulatedActivityLength: Int): BigDecimal = {
    def apportion(amount: BigDecimal): BigDecimal =
      ApportionmentUtils.apportionBasedOnDays(
        amount = amount,
        days = amlRegulatedActivityLength,
        scale = 2,
        roundingMode = RoundingMode.DOWN
      )

    band match {
      case Small     => apportion(appConfig.defaultSmallAmount)
      case Medium    => apportion(appConfig.defaultMediumAmount)
      case Large     => apportion(appConfig.defaultLargeAmount)
      case VeryLarge => apportion(appConfig.defaultVeryLargeAmount)
    }
  }

  def calculateLiability(calculateLiabilityRequest: CalculateLiabilityRequest): CalculatedLiability = {
    val (bands, band) = calculateBand(calculateLiabilityRequest.relevantApLength, calculateLiabilityRequest.ukRevenue)

    val amountDue = calculateAmountDue(band, calculateLiabilityRequest.amlRegulatedActivityLength)

    CalculatedLiability(amountDue, bands, band)
  }

}
