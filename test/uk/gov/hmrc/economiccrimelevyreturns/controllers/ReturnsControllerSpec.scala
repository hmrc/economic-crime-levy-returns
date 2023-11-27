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

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.repositories.{InfoRepository, ReturnsRepository}
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class ReturnsControllerSpec extends SpecBase {

  val mockReturnsRepository: ReturnsRepository = mock[ReturnsRepository]
  val mockInfoRepository: InfoRepository       = mock[InfoRepository]

  val controller = new ReturnsController(
    cc,
    mockReturnsRepository,
    mockInfoRepository,
    fakeAuthorisedAction
  )

  "upsertReturn" should {
    "return 200 OK with the return that was upserted" in {
      when(mockReturnsRepository.upsert(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.upsertReturn()(
          fakeRequestWithJsonBody(Json.toJson(emptyReturn))
        )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyReturn)
    }
  }

  "getReturn" should {
    "return 200 OK with an existing return when there is one for the id" in {
      when(mockReturnsRepository.get(any())).thenReturn(Future.successful(Some(emptyReturn)))

      val result: Future[Result] =
        controller.getReturn("id")(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyReturn)
    }

    "return 404 NOT_FOUND when there is no return for the id" in {
      when(mockReturnsRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.getReturn("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Return not found"))
    }
  }

  "deleteReturn" should {
    "return 204 NO_CONTENT when a return is deleted" in {
      when(mockReturnsRepository.clear(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.deleteReturn("id")(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }
  }

  "upsertInfo" should {
    "return 200 OK with the info that was upserted" in {
      when(mockInfoRepository.upsert(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.upsertInfo()(
          fakeRequestWithJsonBody(Json.toJson(emptyReturn))
        )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyReturn)
    }
  }

  "getInfo" should {
    "return 200 OK with an existing info when there is one for the id" in {
      when(mockInfoRepository.get(any())).thenReturn(Future.successful(Some(emptyInfo)))

      val result: Future[Result] =
        controller.getInfo("id")(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyInfo)
    }

    "return 404 NOT_FOUND when there is no info for the id" in {
      when(mockInfoRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.getInfo("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "No additional info"))
    }
  }

  "deleteInfo" should {
    "return 204 NO_CONTENT when an info is deleted" in {
      when(mockInfoRepository.clear(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.deleteInfo("id")(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }
  }
}
