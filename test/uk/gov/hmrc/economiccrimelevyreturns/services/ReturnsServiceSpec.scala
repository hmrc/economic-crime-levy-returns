/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.ValidEclReturn
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyreturns.repositories.ReturnsRepository

import scala.concurrent.Future

class ReturnsServiceSpec extends SpecBase {

  val mockReturnsRepository: ReturnsRepository = mock[ReturnsRepository]

  "get" should {
    "return an EclReturn when id record is found" in forAll { (validEclReturn: ValidEclReturn, id: String) =>
      reset(mockReturnsRepository)

      when(mockReturnsRepository.get(id))
        .thenReturn(Future.successful(Some(validEclReturn.eclReturn)))

      val sut = new ReturnsService(mockReturnsRepository)

      val result: Either[DataRetrievalError, EclReturn] = await(sut.get(id).value)

      result shouldBe Right(validEclReturn.eclReturn)
    }

    "return DataRetrievalError.NotFound when id record is not found" in forAll { (id: String) =>
      reset(mockReturnsRepository)

      when(mockReturnsRepository.get(id))
        .thenReturn(Future.successful(None))

      val sut = new ReturnsService(mockReturnsRepository)

      val result: Either[DataRetrievalError, EclReturn] = await(sut.get(id).value)

      result shouldBe Left(DataRetrievalError.NotFound(id))
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll { (id: String, errorMessage: String) =>
      reset(mockReturnsRepository)

      val throwable = new Exception(errorMessage)

      when(mockReturnsRepository.get(id))
        .thenReturn(Future.failed(throwable))

      val sut = new ReturnsService(mockReturnsRepository)

      val result: Either[DataRetrievalError, EclReturn] = await(sut.get(id).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(Some(throwable)))
    }
  }

  "delete" should {
    "return a unit when successful" in forAll { (id: String) =>
      reset(mockReturnsRepository)

      when(mockReturnsRepository.delete(id))
        .thenReturn(Future.successful(()))

      val sut = new ReturnsService(mockReturnsRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.delete(id).value)

      result shouldBe Right(())
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll { (id: String, errorMessage: String) =>
      reset(mockReturnsRepository)

      val throwable = new Exception(errorMessage)

      when(mockReturnsRepository.delete(id))
        .thenReturn(Future.failed(throwable))

      val sut = new ReturnsService(mockReturnsRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.delete(id).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(Some(throwable)))
    }
  }

  "upsert" should {
    "return a unit when successful" in forAll { (validEclReturn: ValidEclReturn) =>
      reset(mockReturnsRepository)

      when(mockReturnsRepository.upsert(validEclReturn.eclReturn))
        .thenReturn(Future.successful(()))

      val sut = new ReturnsService(mockReturnsRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.upsert(validEclReturn.eclReturn).value)

      result shouldBe Right(())
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll {
      (validEclReturn: ValidEclReturn, errorMessage: String) =>
        reset(mockReturnsRepository)

        val throwable = new Exception(errorMessage)

        when(mockReturnsRepository.upsert(validEclReturn.eclReturn))
          .thenReturn(Future.failed(throwable))

        val sut = new ReturnsService(mockReturnsRepository)

        val result: Either[DataRetrievalError, Unit] = await(sut.upsert(validEclReturn.eclReturn).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(Some(throwable)))
    }
  }

}
