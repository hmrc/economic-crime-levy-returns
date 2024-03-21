package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually.eventually
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, CustomHeaderNames, FirstTimeReturn}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

class ReturnSubmissionISpec extends ISpecBase {

  private val expectedCallsOnRetry: Int               = 4
  private val getReturnSubmissisonBearerToken: String =
    s"Bearer ${appConfig.integrationFrameworkGetReturnSubmissisonBearerToken}"
  private val integrationFrameworkBearerToken: String =
    s"Bearer ${appConfig.integrationFrameworkBearerToken}"
  private val nrsApiKey: String                       = appConfig.nrsApiKey
  private val returnDate: String                      = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

  s"GET ${routes.ReturnSubmissionController.getSubmission(":periodKey", ":id").url}" should {
    "return 200 OK with an ECL return submission in the JSON response body when the ECL return data is valid" in {
      stubAuthorised()

      val validResponse = random[ValidGetEclReturnSubmissionResponse]

      stubGetSubmission(periodKey, testEclRegistrationReference, validResponse.response)

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.getSubmission(periodKey, testEclRegistrationReference)
        )
      )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(validResponse.response)

      eventually {
        verify(
          1,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$testEclRegistrationReference"))
            .withHeader(HeaderNames.AUTHORIZATION, equalTo(getReturnSubmissisonBearerToken))
            .withHeader(CustomHeaderNames.correlationId, matching(uuidRegex))
            .withHeader(CustomHeaderNames.environment, matching(appConfig.integrationFrameworkEnvironment))
        )

      }
    }

    "retry the get submission call 3 times after the initial attempt if it fails with a 500 INTERNAL_SERVER_ERROR response" in {
      stubAuthorised()

      stubGetSubmissionError(periodKey, testEclRegistrationReference, INTERNAL_SERVER_ERROR, "Internal server error")

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.getSubmission(periodKey, testEclRegistrationReference)
        )
      )

      status(result) shouldBe BAD_GATEWAY

      eventually {
        verify(
          expectedCallsOnRetry,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$testEclRegistrationReference"))
            .withHeader(HeaderNames.AUTHORIZATION, equalTo(getReturnSubmissisonBearerToken))
            .withHeader(CustomHeaderNames.correlationId, matching(uuidRegex))
            .withHeader(CustomHeaderNames.environment, matching(appConfig.integrationFrameworkEnvironment))
        )
      }
    }

    "retry the get submission call 3 times after the initial attempt if it fails with a 502 BAD_GATEWAY response" in {
      stubAuthorised()

      stubGetSubmissionError(periodKey, testEclRegistrationReference, BAD_GATEWAY, "Bad Gateway")

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.getSubmission(periodKey, testEclRegistrationReference)
        )
      )

      status(result) shouldBe BAD_GATEWAY

      eventually {
        verify(
          expectedCallsOnRetry,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$testEclRegistrationReference"))
            .withHeader(HeaderNames.AUTHORIZATION, equalTo(getReturnSubmissisonBearerToken))
            .withHeader(CustomHeaderNames.correlationId, matching(uuidRegex))
            .withHeader(CustomHeaderNames.environment, matching(appConfig.integrationFrameworkEnvironment))
        )
      }
    }
  }

  s"POST ${routes.ReturnSubmissionController.submitReturn(":id").url}"               should {
    "return 200 OK with an ECL return reference number in the JSON response body when the ECL return data is valid " +
      "and is an amend return" in {
        stubAuthorised()

        val validEclReturn               = random[ValidEclReturn]
        val eclReturn                    = validEclReturn.eclReturn.copy(
          amendReason = Some(alphaNumericString),
          base64EncodedDmsSubmissionHtml = Some(base64EncodedDmsSubmissionHtml),
          returnType = Some(AmendReturn)
        )
        val validEclReturnWithReturnType = validEclReturn.copy(eclReturn = eclReturn)

        stubDmsSubmissionSuccess()

        val nrsSubmissionResponse = random[NrsSubmissionResponse]
        stubNrsSuccess(nrsSubmissionResponse)

        callRoute(
          FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(
            Json.toJson(validEclReturnWithReturnType.eclReturn)
          )
        ).futureValue

        val result = callRoute(
          FakeRequest(
            routes.ReturnSubmissionController.submitReturn(validEclReturn.eclReturn.internalId)
          ).withJsonBody(
            Json.toJson(validEclReturn.eclReturn)
          )
        )

        status(result) shouldBe OK

        eventually {
          verify(
            1,
            postRequestedFor(urlEqualTo("/dms-submission/submit"))
              .withHeader(HeaderNames.AUTHORIZATION, equalTo(appConfig.internalAuthToken))
              .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
          )
        }
      }

    "return 200 OK with an ECL return reference number in the JSON response body when the ECL return data is valid " +
      "and is a first time return" in {
        stubAuthorised()

        val validEclReturn               = random[ValidEclReturn]
        val eclReturn                    = validEclReturn.eclReturn.copy(returnType = Some(FirstTimeReturn))
        val validEclReturnWithReturnType = validEclReturn.copy(eclReturn = eclReturn)

        val eclReturnResponse =
          random[SubmitEclReturnResponse].copy(processingDate = Instant.parse("2007-12-25T10:15:30.00Z"))

        stubSubmitEclReturn(
          testEclRegistrationReference,
          validEclReturnWithReturnType.expectedEclReturnSubmission
            .copy(returnDetails =
              validEclReturnWithReturnType.expectedEclReturnSubmission.returnDetails.copy(returnDate = returnDate)
            ),
          eclReturnResponse
        )

        val nrsSubmissionResponse = random[NrsSubmissionResponse]

        stubNrsSuccess(nrsSubmissionResponse)

        callRoute(
          FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(
            Json.toJson(validEclReturnWithReturnType.eclReturn)
          )
        ).futureValue

        val result = callRoute(
          FakeRequest(
            routes.ReturnSubmissionController.submitReturn(validEclReturn.eclReturn.internalId)
          ).withJsonBody(
            Json.toJson(validEclReturn.eclReturn)
          )
        )

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(eclReturnResponse)

        eventually {
          verify(
            1,
            postRequestedFor(urlEqualTo("/submission"))
              .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
              .withHeader(CustomHeaderNames.xApiKey, equalTo(nrsApiKey))
              .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
          )
          verify(
            postRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$testEclRegistrationReference"))
              .withHeader(HeaderNames.AUTHORIZATION, equalTo(integrationFrameworkBearerToken))
              .withHeader(CustomHeaderNames.correlationId, matching(uuidRegex))
              .withHeader(CustomHeaderNames.environment, matching(appConfig.integrationFrameworkBearerToken))
              .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
          )
        }
      }

    "retry the DMS submission call 3 times after the initial attempt if it fails with a 5xx response" in {
      stubAuthorised()

      val validEclReturn               = random[ValidEclReturn]
      val eclReturn                    = validEclReturn.eclReturn.copy(
        amendReason = Some(alphaNumericString),
        base64EncodedDmsSubmissionHtml = Some(base64EncodedDmsSubmissionHtml),
        returnType = Some(AmendReturn)
      )
      val validEclReturnWithReturnType = validEclReturn.copy(eclReturn = eclReturn)

      stubDms5xx()

      val nrsSubmissionResponse = random[NrsSubmissionResponse]
      stubNrsSuccess(nrsSubmissionResponse)

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(
          Json.toJson(validEclReturnWithReturnType.eclReturn)
        )
      ).futureValue

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.submitReturn(validEclReturn.eclReturn.internalId)
        ).withJsonBody(
          Json.toJson(validEclReturn.eclReturn)
        )
      )

      status(result) shouldBe BAD_GATEWAY

      eventually {
        verify(
          expectedCallsOnRetry,
          postRequestedFor(urlEqualTo("/dms-submission/submit"))
            .withHeader(HeaderNames.AUTHORIZATION, equalTo(appConfig.internalAuthToken))
            .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
        )
      }
    }

    "retry the NRS submission call 3 times after the initial attempt if it fails with a 5xx response" in {
      stubAuthorised()

      val validEclReturn               = random[ValidEclReturn]
      val eclReturn                    = validEclReturn.eclReturn.copy(returnType = Some(FirstTimeReturn))
      val validEclReturnWithReturnType = validEclReturn.copy(eclReturn = eclReturn)

      val eclReturnResponse =
        random[SubmitEclReturnResponse].copy(processingDate = Instant.parse("2007-12-25T10:15:30.00Z"))

      stubSubmitEclReturn(
        testEclRegistrationReference,
        validEclReturnWithReturnType.expectedEclReturnSubmission
          .copy(returnDetails =
            validEclReturnWithReturnType.expectedEclReturnSubmission.returnDetails.copy(returnDate = returnDate)
          ),
        eclReturnResponse
      )

      stubNrs5xx()

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(
          Json.toJson(validEclReturnWithReturnType.eclReturn)
        )
      ).futureValue

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.submitReturn(validEclReturnWithReturnType.eclReturn.internalId)
        ).withJsonBody(
          Json.toJson(validEclReturnWithReturnType.eclReturn)
        )
      )

      status(result) shouldBe BAD_GATEWAY

      eventually {
        verify(
          expectedCallsOnRetry,
          postRequestedFor(urlEqualTo("/submission"))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .withHeader(CustomHeaderNames.xApiKey, equalTo(nrsApiKey))
            .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
        )
        verify(
          postRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$testEclRegistrationReference"))
            .withHeader(HeaderNames.AUTHORIZATION, equalTo(integrationFrameworkBearerToken))
            .withHeader(CustomHeaderNames.correlationId, matching(uuidRegex))
            .withHeader(CustomHeaderNames.environment, matching(appConfig.integrationFrameworkBearerToken))
            .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
        )
      }
    }
  }

}
