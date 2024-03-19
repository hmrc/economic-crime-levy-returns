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
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyreturns.repositories.SessionRepository

import scala.concurrent.Future

class SessionServiceSpec extends SpecBase {

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  "get" should {
    "return an SessionData when id record is found" in forAll { (sessionData: SessionData, id: String) =>
      reset(mockSessionRepository)

      when(mockSessionRepository.get(id))
        .thenReturn(Future.successful(Some(sessionData)))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, SessionData] = await(sut.get(id).value)

      result shouldBe Right(sessionData)
    }

    "return DataRetrievalError.NotFound when id record is not found" in forAll { (id: String) =>
      reset(mockSessionRepository)

      when(mockSessionRepository.get(id))
        .thenReturn(Future.successful(None))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, SessionData] = await(sut.get(id).value)

      result shouldBe Left(DataRetrievalError.NotFound(id))
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll { (id: String, errorMessage: String) =>
      reset(mockSessionRepository)

      val throwable = new Exception(errorMessage)

      when(mockSessionRepository.get(id))
        .thenReturn(Future.failed(throwable))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, SessionData] = await(sut.get(id).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(Some(throwable)))
    }
  }

  "delete" should {
    "return a unit when successful" in forAll { (id: String) =>
      reset(mockSessionRepository)

      when(mockSessionRepository.deleteRecord(id))
        .thenReturn(Future.successful(()))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.delete(id).value)

      result shouldBe Right(())
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll { (id: String, errorMessage: String) =>
      reset(mockSessionRepository)

      val throwable = new Exception(errorMessage)

      when(mockSessionRepository.deleteRecord(id))
        .thenReturn(Future.failed(throwable))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.delete(id).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(Some(throwable)))
    }
  }

  "upsert" should {
    "return a unit when successful" in forAll { (sessionData: SessionData) =>
      reset(mockSessionRepository)

      when(mockSessionRepository.upsert(sessionData))
        .thenReturn(Future.successful(()))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.upsert(sessionData).value)

      result shouldBe Right(())
    }

    "return DataRetrievalError.InternalUnexpectedError when error" in forAll { (sessionData: SessionData, errorMessage: String) =>
      reset(mockSessionRepository)

      val throwable = new Exception(errorMessage)

      when(mockSessionRepository.upsert(sessionData))
        .thenReturn(Future.failed(throwable))

      val sut = new SessionService(mockSessionRepository)

      val result: Either[DataRetrievalError, Unit] = await(sut.upsert(sessionData).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(Some(throwable)))
    }
  }
}
