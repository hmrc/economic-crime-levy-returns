package uk.gov.hmrc.economiccrimelevyreturns.base

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object WireMockHelper {

  val wireMockPort = 9999
  val wireMockHost = "localhost"
  val url          = s"http://$wireMockHost:$wireMockPort"

  def setWireMockPort(services: String*): Map[String, Any] =
    services.foldLeft(Map.empty[String, Any]) { case (map, service) =>
      map + (s"microservice.services.$service.port" -> wireMockPort)
    }

  def stub(method: MappingBuilder, response: ResponseDefinitionBuilder): StubMapping =
    stubFor(method.willReturn(response))

  def stubGet(uri: String, responseBody: String): StubMapping =
    stub(get(urlEqualTo(uri)), okJson(responseBody))

  def stubPost(
    url: String,
    responseStatus: Int,
    responseBody: String,
    responseHeader: (String, String) = ("", "")
  ): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      post(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        )
    )
  }

  def stubDelete(
    url: String,
    responseStatus: Int,
    responseBody: String,
    responseHeader: (String, String) = ("", "")
  ): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(
      delete(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        )
    )
  }

  def stubPut(
    url: String,
    responseStatus: Int,
    responseBody: String,
    responseHeader: (String, String) = ("", "")
  ): StubMapping = {
    removeStub(put(urlMatching(url)))
    stubFor(
      put(urlMatching(url))
        .willReturn(
          aResponse().withStatus(responseStatus).withBody(responseBody).withHeader(responseHeader._1, responseHeader._2)
        )
    )
  }

}

trait WireMockHelper {

  import WireMockHelper._

  lazy val wireMockConfiguration: WireMockConfiguration = wireMockConfig().port(wireMockPort)
  lazy val wireMockServer                               = new WireMockServer(wireMockConfiguration)

  def startWireMock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  def stopWireMock(): Unit = wireMockServer.stop()

  def resetWireMock(): Unit = WireMock.reset()

}
