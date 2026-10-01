/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.connectors

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.economiccrimelevyreturns.ValidGetEclReturnSubmissionResponse
import uk.gov.hmrc.economiccrimelevyreturns.base.SpecBase
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries.*
import uk.gov.hmrc.economiccrimelevyreturns.models.hip.{HipSubmitReturn, HipSubmitReturnSuccessResponse}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.EclReturnSubmission
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class HipReturnsConnectorSpec extends SpecBase {

  val retryAmount                        = 4
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val connector = new HipReturnsConnector(appConfig, mockHttpClient, config, actorSystem)

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  private def stubHeaders(): Unit =
    when(mockRequestBuilder.setHeader(any(), any(), any(), any(), any(), any())).thenReturn(mockRequestBuilder)

  private def stubGet(response: HttpResponse): Unit = {
    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
    stubHeaders()
    when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))
  }

  private def stubPost(response: HttpResponse): Unit = {
    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
    stubHeaders()
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))
  }

  private def wrapped(body: JsValue): String =
    Json.stringify(Json.obj("success" -> body))

  "getEclReturnSubmission" should {
    "return the return submission when the response is wrapped in a success envelope" in forAll {
      (validResponse: ValidGetEclReturnSubmissionResponse) =>
        beforeEach()

        stubGet(HttpResponse.apply(OK, wrapped(Json.toJson(validResponse.response))))

        val result = await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))

        result shouldBe validResponse.response
    }

    "fail to parse a success response that is not wrapped in a success envelope" in forAll {
      (validResponse: ValidGetEclReturnSubmissionResponse) =>
        beforeEach()

        stubGet(HttpResponse.apply(OK, Json.stringify(Json.toJson(validResponse.response))))

        Try(await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))) match {
          case Failure(e) => e shouldBe a[JsResult.Exception]
          case _          => fail("expected parsing to fail when the success envelope is absent")
        }
    }

    "return 400 UpstreamErrorResponse without retrying when downstream call returns an error" in {
      beforeEach()

      val errorCode = BAD_REQUEST

      stubGet(HttpResponse.apply(errorCode, "BAD_REQUEST"))

      Try(await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when error is received")
      }

      verify(mockRequestBuilder, times(1)).execute(any(), any())
    }

    "return 422 UpstreamErrorResponse without retrying when downstream call returns a business validation error" in {
      beforeEach()

      val errorCode = UNPROCESSABLE_ENTITY

      stubGet(HttpResponse.apply(errorCode, "UNPROCESSABLE_ENTITY"))

      Try(await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when error is received")
      }

      verify(mockRequestBuilder, times(1)).execute(any(), any())
    }

    "return 500 UpstreamErrorResponse when downstream call returns an error and executes retry" in {
      beforeEach()

      val errorCode = INTERNAL_SERVER_ERROR

      stubGet(HttpResponse.apply(errorCode, "Internal server error"))

      Try(await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received")
      }

      verify(mockRequestBuilder, times(retryAmount)).execute(any(), any())
    }

    "return 503 UpstreamErrorResponse when downstream call returns an error and executes retry" in {
      beforeEach()

      val errorCode = SERVICE_UNAVAILABLE

      stubGet(HttpResponse.apply(errorCode, "SERVICE_UNAVAILABLE"))

      Try(await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))) match {
        case Failure(UpstreamErrorResponse(_, code, _, _)) =>
          code shouldEqual errorCode
        case _                                             => fail("expected UpstreamErrorResponse when an error is received")
      }

      verify(mockRequestBuilder, times(retryAmount)).execute(any(), any())
    }
  }

  "submitEclReturn" should {
    "return the integration framework shaped response from a 201, dropping the hip only fields" in forAll {
      (eclReturnSubmission: EclReturnSubmission, hipSubmitReturn: HipSubmitReturn) =>
        beforeEach()

        stubPost(
          HttpResponse.apply(
            CREATED,
            Json.stringify(Json.toJson(HipSubmitReturnSuccessResponse(hipSubmitReturn)))
          )
        )

        val result = await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))

        result shouldBe hipSubmitReturn.toIfResponse
        result.processingDate  shouldBe hipSubmitReturn.processingDate
        result.chargeReference shouldBe hipSubmitReturn.chargeReference
    }

    "fail to parse a 201 that is not wrapped in a success envelope" in forAll {
      (eclReturnSubmission: EclReturnSubmission, hipSubmitReturn: HipSubmitReturn) =>
        beforeEach()

        stubPost(HttpResponse.apply(CREATED, Json.stringify(Json.toJson(hipSubmitReturn))))

        Try(await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))) match {
          case Failure(e) => e shouldBe a[JsResult.Exception]
          case _          => fail("expected parsing to fail when the success envelope is absent")
        }
    }

    "return 401 UpstreamErrorResponse without retrying when downstream call returns an error" in forAll {
      (eclReturnSubmission: EclReturnSubmission) =>
        beforeEach()

        val errorCode = UNAUTHORIZED

        stubPost(HttpResponse.apply(errorCode, "Failed authorization"))

        Try(await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received")
        }

        verify(mockRequestBuilder, times(1)).execute(any(), any())
    }

    "return 422 UpstreamErrorResponse without retrying when downstream call returns a business validation error" in forAll {
      (eclReturnSubmission: EclReturnSubmission) =>
        beforeEach()

        val errorCode = UNPROCESSABLE_ENTITY

        stubPost(HttpResponse.apply(errorCode, "UNPROCESSABLE_ENTITY"))

        Try(await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received")
        }

        verify(mockRequestBuilder, times(1)).execute(any(), any())
    }

    "return 500 UpstreamErrorResponse when downstream call returns an error and executes retry" in forAll {
      (eclReturnSubmission: EclReturnSubmission) =>
        beforeEach()

        val errorCode = INTERNAL_SERVER_ERROR

        stubPost(HttpResponse.apply(errorCode, "Internal server error"))

        Try(await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))) match {
          case Failure(UpstreamErrorResponse(_, code, _, _)) =>
            code shouldEqual errorCode
          case _                                             => fail("expected UpstreamErrorResponse when an error is received")
        }

        verify(mockRequestBuilder, times(retryAmount)).execute(any(), any())
    }
  }
}
