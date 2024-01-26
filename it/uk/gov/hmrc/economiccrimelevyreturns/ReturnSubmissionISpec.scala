package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, postRequestedFor, urlEqualTo, verify}
import org.scalatest.concurrent.Eventually.eventually
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.FirstTimeReturn
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

class ReturnSubmissionISpec extends ISpecBase {

  private val expectedCallsOnRetry = 4
  private val returnDate           = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

  s"GET ${routes.ReturnSubmissionController.getSubmission(":periodKey", ":id").url}" should {
    "return 200 OK with an ECL return submission in the JSON response body when the ECL return data is valid" in {
      stubAuthorised()

      val validResponse = random[ValidGetEclReturnSubmissionResponse]

      stubGetSubmission(periodKey, eclRegistrationReference, validResponse.response)

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.getSubmission(periodKey, eclRegistrationReference)
        )
      )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(validResponse.response)

      eventually {
        verify(1, getRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$eclRegistrationReference")))
      }
    }

    "retry the get submission call 3 times after the initial attempt if it fails with a 500 INTERNAL_SERVER_ERROR response" in {
      stubAuthorised()

      stubGetSubmissionError(periodKey, eclRegistrationReference, INTERNAL_SERVER_ERROR, "Internal server error")

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.getSubmission(periodKey, eclRegistrationReference)
        )
      )

      status(result) shouldBe BAD_GATEWAY

      eventually {
        verify(
          expectedCallsOnRetry,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$eclRegistrationReference"))
        )
      }
    }

    "retry the get submission call 3 times after the initial attempt if it fails with a 502 BAD_GATEWAY response" in {
      stubAuthorised()

      stubGetSubmissionError(periodKey, eclRegistrationReference, BAD_GATEWAY, "Bad Gateway")

      val result = callRoute(
        FakeRequest(
          routes.ReturnSubmissionController.getSubmission(periodKey, eclRegistrationReference)
        )
      )

      status(result) shouldBe BAD_GATEWAY

      eventually {
        verify(
          expectedCallsOnRetry,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$eclRegistrationReference"))
        )
      }
    }
  }

  s"POST ${routes.ReturnSubmissionController.submitReturn(":id").url}"               should {
    "return 200 OK with an ECL return reference number in the JSON response body when the ECL return data is valid" in {
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
        verify(1, postRequestedFor(urlEqualTo("/submission")))
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
        verify(expectedCallsOnRetry, postRequestedFor(urlEqualTo("/submission")))
      }
    }
  }

}
