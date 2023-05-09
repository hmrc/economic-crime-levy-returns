package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper.stub
import uk.gov.hmrc.economiccrimelevyreturns.models.nrs.NrsSubmissionResponse

trait NrsStubs { self: WireMockStubs =>
  def stubNrsSuccess(nrsSubmissionResponse: NrsSubmissionResponse): StubMapping =
    stub(
      post(urlEqualTo("/submission")),
      aResponse()
        .withStatus(ACCEPTED)
        .withBody(Json.toJson(nrsSubmissionResponse).toString())
    )

  def stubNrs5xx(): StubMapping =
    stub(
      post(urlEqualTo("/submission")),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("Internal server error")
    )

}
