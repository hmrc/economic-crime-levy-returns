package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.eacd.EclEnrolment

trait AuthStubs { self: WireMockStubs =>
  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
         """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(
          s"""
             |{
             |  "internalId": "$testInternalId",
             |  "authorisedEnrolments": [{
             |    "key":"${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationReference" }],
             |    "state": "activated"
             |  }]
             |}
         """.stripMargin)
    )

}
