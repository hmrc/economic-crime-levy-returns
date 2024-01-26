package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, GetEclReturnSubmissionResponse, SubmitEclReturnResponse}

trait IntegrationFrameworkStubs { self: WireMockStubs =>

  def stubGetSubmission(
    periodKey: String,
    eclRegistrationReference: String,
    response: GetEclReturnSubmissionResponse
  ): StubMapping =
    stubGet(
      s"/economic-crime-levy/return/$periodKey/$eclRegistrationReference",
      Json.toJson(response).toString()
    )

  def stubGetSubmissionError(
    periodKey: String,
    eclRegistrationReference: String,
    statusCode: Int,
    errorMessage: String
  ): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy/return/$periodKey/$eclRegistrationReference")),
      aResponse()
        .withStatus(statusCode)
        .withBody(errorMessage)
    )

  def stubSubmitEclReturn(
    eclRegistrationReference: String,
    eclReturnSubmission: EclReturnSubmission,
    eclReturnResponse: SubmitEclReturnResponse
  ): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy/return/$eclRegistrationReference"))
        .withRequestBody(equalToJson(Json.toJson(eclReturnSubmission).toString())),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(eclReturnResponse).toString())
    )

}
