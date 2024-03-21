package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper.stub

trait DmsStubs { self: WireMockStubs =>

  def stubDmsSubmissionSuccess(): StubMapping =
    stub(
      post(urlEqualTo("/dms-submission/submit")),
      aResponse()
        .withStatus(ACCEPTED)
    )

  def stubDms5xx(): StubMapping =
    stub(
      post(urlEqualTo("/dms-submission/submit")),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("Internal server error")
    )
}
