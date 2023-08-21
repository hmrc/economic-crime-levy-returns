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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, stubControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.models.dms.DmsNotification
import uk.gov.hmrc.economiccrimelevyreturns.services.{DmsService, NrsService}
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class DmsNotificationControllerSpec extends SpecBase {

  val mockNrsService: NrsService                       = mock[NrsService]
  val mockDmsService: DmsService                       = mock[DmsService]
  val mockStubBehaviour: StubBehaviour                 = mock[StubBehaviour]
  val stubBackendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  val controller = new DmsNotificationController(
    cc,
    stubBackendAuthComponents,
    appConfig
  )

  "dmsCallback" should {
    "return OK when receiving a correct notifications from DMS" in forAll { dmsNotification: DmsNotification =>
      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.unit)

      val request = FakeRequest(POST, routes.DmsNotificationController.dmsCallback().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.toJson(dmsNotification))

      val result = controller.dmsCallback()(request)
      status(result) shouldBe OK
    }

    "return BAD_REQUEST when an invalid request is received" in {
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest(POST, routes.DmsNotificationController.dmsCallback().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.obj())

      val result = controller.dmsCallback()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "fail for an unauthenticated user" in {
      val request = FakeRequest(POST, routes.DmsNotificationController.dmsCallback().url)
        .withBody(Json.toJson(random[DmsNotification])) // No Authorization header

      val result = controller.dmsCallback()(request)
      Try(status(result)) match {
        case Success(_) => fail()
        case Failure(_) =>
      }
    }

    "fail when the user is not authorised" in {
      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val request = FakeRequest(POST, routes.DmsNotificationController.dmsCallback().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.toJson(random[DmsNotification]))

      val result = controller.dmsCallback()(request)
      Try(status(result)) match {
        case Success(_) => fail()
        case Failure(_) =>
      }
    }
  }
}
