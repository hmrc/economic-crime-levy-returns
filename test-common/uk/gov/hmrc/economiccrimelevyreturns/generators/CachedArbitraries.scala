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

package uk.gov.hmrc.economiccrimelevyreturns.generators

import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary
import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import uk.gov.hmrc.economiccrimelevyreturns.models.{Band, CalculatedLiability, ReturnType}
import uk.gov.hmrc.economiccrimelevyreturns.models.des.{ObligationStatus => DesObligationStatus}
import uk.gov.hmrc.economiccrimelevyreturns.models.ObligationStatus
import uk.gov.hmrc.economiccrimelevyreturns.models.dms.DmsNotification
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ErrorCode

object CachedArbitraries extends EclTestData with Generators {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbObligationStatus: Arbitrary[ObligationStatus]               = mkArb
  implicit lazy val arbDesObligationStatus: Arbitrary[DesObligationStatus]         = mkArb
  implicit lazy val arbCalculatedLiability: Arbitrary[CalculatedLiability]         = mkArb
  implicit lazy val arbSubmitEclReturnResponse: Arbitrary[SubmitEclReturnResponse] = mkArb
  implicit lazy val arbEclReturnSubmission: Arbitrary[EclReturnSubmission]         = mkArb
  implicit lazy val arbNrsIdentityData: Arbitrary[NrsIdentityData]                 = mkArb
  implicit lazy val arbNrsSubmission: Arbitrary[NrsSubmission]                     = mkArb
  implicit lazy val arbNrsSubmissionResponse: Arbitrary[NrsSubmissionResponse]     = mkArb
  implicit lazy val arbDmsNotification: Arbitrary[DmsNotification]                 = mkArb
  implicit lazy val arbErrorCode: Arbitrary[ErrorCode]                             = mkArb
  implicit lazy val arbReturnType: Arbitrary[ReturnType]                           = mkArb
  implicit lazy val arbBand: Arbitrary[Band]                                       = mkArb

}
