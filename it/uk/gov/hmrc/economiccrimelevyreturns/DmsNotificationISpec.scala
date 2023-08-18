package uk.gov.hmrc.economiccrimelevyreturns

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyreturns.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyreturns.controllers.routes
import uk.gov.hmrc.economiccrimelevyreturns.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyreturns.models.dms.DmsNotification


class DmsNotificationISpec extends ISpecBase {
  s"POST ${routes.DmsNotificationController.dmsCallback().url}" should {
    "process a notification message from DMS" in {
      stubInternalAuthorised()

      val result = callRoute(
        FakeRequest(routes.DmsNotificationController.dmsCallback())
          .withJsonBody(Json.toJson(random[DmsNotification]))
          .withHeaders(AUTHORIZATION -> "Token some-token")
      )

      status(result)        shouldBe OK
    }
  }

}
