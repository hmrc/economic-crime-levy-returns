package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}

trait IntegrationFrameworkStubs { self: WireMockStubs =>

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
