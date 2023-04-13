package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse

import java.time.Instant

class ReturnSubmissionISpec extends ISpecBase {
  s"POST ${routes.ReturnSubmissionController.submitReturn(":id").url}" should {
    "return 200 OK with an ECL return reference number in the JSON response body when the ECL return data is valid" in {
      stubAuthorised()

      val validEclReturn    = random[ValidEclReturn]
      val eclReturnResponse =
        random[SubmitEclReturnResponse].copy(processingDate = Instant.parse("2007-12-25T10:15:30.00Z"))

      stubSubmitEclReturn(
        testEclRegistrationReference,
        validEclReturn.expectedEclReturnDetails.periodKey,
        validEclReturn.expectedEclReturnDetails,
        eclReturnResponse
      )

      callRoute(
        FakeRequest(routes.ReturnsController.upsertReturn).withJsonBody(
          Json.toJson(validEclReturn.eclReturn)
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
    }
  }

}
