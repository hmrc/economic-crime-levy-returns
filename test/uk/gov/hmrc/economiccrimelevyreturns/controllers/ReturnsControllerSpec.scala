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
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.{DataRetrievalError, ResponseError}
import uk.gov.hmrc.economiccrimelevyreturns.services.ReturnsService
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class ReturnsControllerSpec extends SpecBase {

  val mockReturnsService: ReturnsService = mock[ReturnsService]

  val controller = new ReturnsController(
    cc,
    mockReturnsService,
    fakeAuthorisedAction
  )

  "upsertReturn" should {
    "return 200 OK with the return that was upserted" in {
      when(mockReturnsService.upsert(any()))
        .thenReturn(EitherT.rightT[Future, DataRetrievalError](()))

      val result: Future[Result] =
        controller.upsertReturn()(
          fakeRequestWithJsonBody(Json.toJson(emptyReturn))
        )

      status(result) shouldBe NO_CONTENT
    }
  }

  "getReturn" should {
    "return 200 OK with an existing return when there is one for the id" in {
      when(mockReturnsService.get(any()))
        .thenReturn(EitherT.rightT[Future, DataRetrievalError](emptyReturn))

      val result: Future[Result] =
        controller.getReturn("id")(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyReturn)
    }

    "return 404 NOT_FOUND when there is no return for the id" in {
      when(mockReturnsService.get(any()))
        .thenReturn(EitherT.leftT[Future, EclReturn](DataRetrievalError.NotFound("id")))

      val result: Future[Result] =
        controller.getReturn("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(ResponseError.notFoundError("Unable to find record with id: id"))
    }
  }

  "deleteReturn" should {
    "return 204 NO_CONTENT when a return is deleted" in {
      when(mockReturnsService.delete(anyString()))
        .thenReturn(EitherT.rightT[Future, DataRetrievalError](()))

      val result: Future[Result] =
        controller.deleteReturn("id")(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }
  }

}
