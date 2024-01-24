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

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataRetrievalError, ResponseError}
import uk.gov.hmrc.economiccrimelevyreturns.services.SessionService

import scala.concurrent.Future

class SessionControllerSpec extends SpecBase {

  val mockSessionService: SessionService = mock[SessionService]

  val controller = new SessionController(
    cc,
    mockSessionService,
    fakeAuthorisedAction
  )

  "upsertSession" should {
    "return 200 OK with the session that was upserted" in forAll { sessionData: SessionData =>
      when(mockSessionService.upsert(ArgumentMatchers.eq(sessionData)))
        .thenReturn(EitherT.rightT[Future, DataRetrievalError](()))

      val result: Future[Result] =
        controller.upsert()(
          fakeRequestWithJsonBody(Json.toJson(sessionData))
        )

      status(result) shouldBe OK
    }

    "return 404 NOT_FOUND when there is no SessionData for the id" in forAll { sessionData: SessionData =>
      when(mockSessionService.upsert(ArgumentMatchers.eq(sessionData)))
        .thenReturn(EitherT.leftT[Future, Unit](DataRetrievalError.NotFound(sessionData.internalId)))

      val result: Future[Result] =
        controller.upsert()(
          fakeRequestWithJsonBody(Json.toJson(sessionData))
        )

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${sessionData.internalId}")
      )
    }

    "return 500 INTERNAL_SERVER_ERROR when SessionData retrieval fails" in forAll { sessionData: SessionData =>
      when(mockSessionService.upsert(ArgumentMatchers.eq(sessionData)))
        .thenReturn(EitherT.leftT[Future, Unit](DataRetrievalError.InternalUnexpectedError(None)))

      val result: Future[Result] =
        controller.upsert()(fakeRequestWithJsonBody(Json.toJson(sessionData)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getSessionData" should {
    "return 200 OK with an existing SessionData when there is one for the id" in forAll { sessionData: SessionData =>
      when(mockSessionService.get(ArgumentMatchers.eq(sessionData.internalId)))
        .thenReturn(EitherT.rightT[Future, DataRetrievalError](sessionData))

      val result: Future[Result] =
        controller.get(sessionData.internalId)(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(sessionData)
    }

    "return 404 NOT_FOUND when there is no SessionData for the id" in { sessionData: SessionData =>
      when(mockSessionService.get(ArgumentMatchers.eq(sessionData.internalId)))
        .thenReturn(EitherT.leftT[Future, SessionData](DataRetrievalError.NotFound(sessionData.internalId)))

      val result: Future[Result] =
        controller.get(sessionData.internalId)(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${sessionData.internalId}")
      )
    }

    "return 500 INTERNAL_SERVER_ERROR when SessionData retrieval fails" in { sessionData: SessionData =>
      when(mockSessionService.get(ArgumentMatchers.eq(sessionData.internalId)))
        .thenReturn(EitherT.leftT[Future, SessionData](DataRetrievalError.InternalUnexpectedError(None)))

      val result: Future[Result] =
        controller.get(sessionData.internalId)(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "deleteSessionData" should {
    "return 200 OK when deletion of SessionData with the given id is successful" in { sessionData: SessionData =>
      when(mockSessionService.delete(ArgumentMatchers.eq(sessionData.internalId)))
        .thenReturn(EitherT.rightT[Future, DataRetrievalError](()))

      val result: Future[Result] =
        controller.delete(sessionData.internalId)(fakeRequest)

      status(result) shouldBe OK
    }

    "return 404 NOT_FOUND when there is no SessionData for the id" in { sessionData: SessionData =>
      when(mockSessionService.delete(ArgumentMatchers.eq(sessionData.internalId)))
        .thenReturn(EitherT.leftT[Future, Unit](DataRetrievalError.NotFound("id")))

      val result: Future[Result] =
        controller.delete(sessionData.internalId)(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${sessionData.internalId}")
      )
    }

    "return 500 INTERNAL_SERVER_ERROR when SessionData deletion fails" in { sessionData: SessionData =>
      when(mockSessionService.delete(ArgumentMatchers.eq(sessionData.internalId)))
        .thenReturn(EitherT.leftT[Future, Unit](DataRetrievalError.InternalUnexpectedError(None)))

      val result: Future[Result] =
        controller.delete(sessionData.internalId)(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
