/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyreturns.base

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Status => _, _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test._
import play.api.{Application, Mode}
import uk.gov.hmrc.economiccrimelevyreturns.EclTestData
import uk.gov.hmrc.economiccrimelevyreturns.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

abstract class ISpecBase
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with Inspectors
    with ScalaFutures
    with DefaultAwaitTimeout
    with Writeables
    with EssentialActionCaller
    with RouteInvokers
    with LoneElement
    with Inside
    with OptionValues
    with Results
    with Status
    with HeaderNames
    with MimeTypes
    with HttpProtocol
    with HttpVerbs
    with ResultExtractors
    with WireMockHelper
    with WireMockStubs
    with IntegrationPatience
    with EclTestData {

  implicit lazy val system: ActorSystem        = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit def ec: ExecutionContext            = global

  val internalId: String       = "test-id"
  val emptyReturn: EclReturn   = EclReturn.empty(internalId)
  val now: Instant             = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"    -> false,
    "auditing.enabled"   -> false,
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  ) ++ setWireMockPort(
    "auth"
  )

  val contextPath: String = "economic-crime-levy-returns"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(additionalAppConfig)
      .overrides(bind(classOf[Clock]).toInstance(stubClock))
      .in(Mode.Test)
      .build()

  /*
  This is to initialise the app before running any tests, as it is lazy by default in org.scalatestplus.play.BaseOneAppPerSuite.
  It enables us to include behaviour tests that call routes within the `should` part of a test but before `in`.
   */
  locally { val _ = app }

  override def beforeAll(): Unit = {
    startWireMock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    resetWireMock()
    callRoute(
      FakeRequest(uk.gov.hmrc.economiccrimelevyreturns.controllers.test.routes.TestController.clearAllData),
      requiresAuth = false
    ).futureValue
    super.afterEach()
  }

  def callRoute[A](fakeRequest: FakeRequest[A], requiresAuth: Boolean = true)(implicit
    app: Application,
    w: Writeable[A]
  ): Future[Result] = {
    val errorHandler = app.errorHandler

    val req = if (requiresAuth) fakeRequest.withHeaders("Authorization" -> "test") else fakeRequest

    route(app, req) match {
      case None         => fail("Route does not exist")
      case Some(result) =>
        result.recoverWith { case t: Throwable =>
          errorHandler.onServerError(req, t)
        }
    }
  }

}
