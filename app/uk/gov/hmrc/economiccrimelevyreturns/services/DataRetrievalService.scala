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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyreturns.repositories.ReturnsRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataRetrievalService @Inject() (
  returnsRepository: ReturnsRepository
) {
  def get(id: String)(implicit ec: ExecutionContext): EitherT[Future, DataRetrievalError, EclReturn] =
    EitherT {
      returnsRepository.get(id).map {
        case Some(value) => Right(value)
        case None        => Left(DataRetrievalError.NotFound(id))
      }
    }
}
