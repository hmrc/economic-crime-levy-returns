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
import org.scalatest.prop.TableFor4
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

  private def envelope(origin: String, response: JsValue): String =
    Json.stringify(Json.obj("origin" -> origin, "response" -> response))

  private def systemError(code: String, message: String): JsValue =
    Json.obj("error" -> Json.obj("code" -> code, "logID" -> "D82EBAB67AC6D7565C0682CA91BDC577", "message" -> message))

  private val hipFailures: JsValue =
    Json.obj("failures" -> Json.arr(Json.obj("type" -> "Type of Failure", "reason" -> "Reason for Failure")))

  private val businessValidationError: String =
    Json.stringify(
      Json.obj(
        "error" -> Json.obj(
          "errorId"        -> "044",
          "processingDate" -> "2022-01-31T09:26:17Z",
          "text"           -> "Tax Obligation Already Fulfilled"
        )
      )
    )

  private val errorCases: TableFor4[Int, String, String, Int] = Table(
    ("status", "body", "message", "attempts"),
    (
      BAD_REQUEST,
      envelope("HoD", systemError("400", "Submission has not passed validation.")),
      "origin: HoD, HIP system error code: 400, logID: D82EBAB67AC6D7565C0682CA91BDC577, " +
        "message: Submission has not passed validation.",
      1
    ),
    (
      BAD_REQUEST,
      envelope("HIP", hipFailures),
      "origin: HIP, HIP failures: [Type of Failure - Reason for Failure]",
      1
    ),
    (
      INTERNAL_SERVER_ERROR,
      envelope("HoD", systemError("500", "Internal Server Error")),
      "origin: HoD, HIP system error code: 500",
      retryAmount
    ),
    (
      SERVICE_UNAVAILABLE,
      envelope("HIP", hipFailures),
      "origin: HIP, HIP failures: [Type of Failure - Reason for Failure]",
      retryAmount
    ),
    (
      UNPROCESSABLE_ENTITY,
      businessValidationError,
      "HIP 422 errorId: 044, processingDate: 2022-01-31T09:26:17Z, text: Tax Obligation Already Fulfilled",
      1
    ),
    (
      BAD_REQUEST,
      "not json",
      "origin: unknown, HIP error response in unexpected format: status: 400 body: not json",
      1
    ),
    (
      SERVICE_UNAVAILABLE,
      envelope("HIP", Json.obj("other" -> 1)),
      "HIP error response in unexpected format: status: 503",
      retryAmount
    ),
    (UNPROCESSABLE_ENTITY, "{}", "HIP 422 in unexpected format: {}", 1),
    (UNAUTHORIZED, "", "", 1),
    (NOT_FOUND, "", "", 1)
  )

  private def assertUpstreamError(
    call: => Future[?],
    expectedStatus: Int,
    expectedMessage: String,
    expectedAttempts: Int
  ): Unit = {
    Try(await(call)) match {
      case Failure(UpstreamErrorResponse(message, status, _, _)) =>
        status shouldEqual expectedStatus
        message     should include(expectedMessage)
      case other                                                 =>
        fail(s"expected UpstreamErrorResponse, got $other")
    }

    verify(mockRequestBuilder, times(expectedAttempts)).execute(any(), any())
  }

  "getEclReturnSubmission" should {
    "return the integration framework shaped return submission when the response is wrapped in a success envelope from HIP" in {
      (validResponse: ValidGetEclReturnSubmissionResponse) =>
        beforeEach()

        stubGet(HttpResponse.apply(OK, wrapped(Json.toJson(validResponse.response))))

        val result = await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))

        result shouldBe validResponse.response
    }

    "fail to parse a success response that is not wrapped in a success envelope" in {
      (validResponse: ValidGetEclReturnSubmissionResponse) =>
        beforeEach()

        stubGet(HttpResponse.apply(OK, Json.stringify(Json.toJson(validResponse.response))))

        Try(await(connector.getEclReturnSubmission(periodKey, testEclRegistrationReference))) match {
          case Failure(e) => e shouldBe a[JsResult.Exception]
          case _          => fail("expected parsing to fail when the success envelope is absent")
        }
    }

    "keep the original status, describe the HIP error body and retry only on 5xx" in
      forAll(errorCases) { (status, body, message, attempts) =>
        beforeEach()

        stubGet(HttpResponse.apply(status, body))

        assertUpstreamError(
          connector.getEclReturnSubmission(periodKey, testEclRegistrationReference),
          status,
          message,
          attempts
        )
      }
  }

  "submitEclReturn" should {
    "return the integration framework shaped response from a 201, dropping the hip only fields" in {
      (eclReturnSubmission: EclReturnSubmission, hipSubmitReturn: HipSubmitReturn) =>
        beforeEach()

        stubPost(
          HttpResponse.apply(
            CREATED,
            Json.stringify(Json.toJson(HipSubmitReturnSuccessResponse(hipSubmitReturn)))
          )
        )

        val result = await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))

        result                 shouldBe hipSubmitReturn.toIfResponse
        result.processingDate  shouldBe hipSubmitReturn.processingDate
        result.chargeReference shouldBe hipSubmitReturn.chargeReference
    }

    "fail to parse a 201 that is not wrapped in a success envelope" in {
      (eclReturnSubmission: EclReturnSubmission, hipSubmitReturn: HipSubmitReturn) =>
        beforeEach()

        stubPost(HttpResponse.apply(CREATED, Json.stringify(Json.toJson(hipSubmitReturn))))

        Try(await(connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission))) match {
          case Failure(e) => e shouldBe a[JsResult.Exception]
          case _          => fail("expected parsing to fail when the success envelope is absent")
        }
    }

    "keep the original status, describe the HIP error body and retry only on 5xx" in {
      val eclReturnSubmission = random[EclReturnSubmission]

      forAll(errorCases) { (status, body, message, attempts) =>
        beforeEach()

        stubPost(HttpResponse.apply(status, body))

        assertUpstreamError(
          connector.submitEclReturn(testEclRegistrationReference, eclReturnSubmission),
          status,
          message,
          attempts
        )
      }
    }
  }

  "parseHipErrorEnvelopeResponse" should {
    "reject a status that HIP does not wrap in an origin envelope" in {
      an[IllegalArgumentException] should be thrownBy
        connector.parseHipErrorEnvelopeResponse(HttpResponse.apply(UNPROCESSABLE_ENTITY, businessValidationError))
    }
  }
}
