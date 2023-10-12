package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment

trait AuthStubs { self: WireMockStubs =>
  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "$testInternalId",
             |  "authorisedEnrolments": [{
             |    "key":"${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationReference" }],
             |    "state": "activated"
             |  }],
             |  "loginTimes": {
             |     "currentLogin": "2016-11-27T09:00:00.000Z",
             |     "previousLogin": "2016-11-01T12:00:00.000Z"
             |  },
             |  "agentInformation": {},
             |  "confidenceLevel": 50
             |}
         """.stripMargin)
    )
}
