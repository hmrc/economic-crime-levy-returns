package uk.gov.hmrc.economiccrimelevyreturns.repositories

import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.EclReturn
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class ReturnsRepositorySpec
    extends AnyWordSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EclReturn]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val now              = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)
  private val eclReturn        = EclReturn.empty("test-id").copy(lastUpdated = Some(Instant.ofEpochSecond(1)))
  private val mockAppConfig    = mock[AppConfig]

  when(mockAppConfig.mongoTtl) thenReturn 1

  protected override val repository = new ReturnsRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  "upsert" should {
    "insert a new return with the last updated time set to `now`" in {
      val expectedResult = eclReturn.copy(lastUpdated = Some(now))

      val setResult: Unit = repository.upsert(eclReturn).futureValue
      val updatedRecord   = find(Filters.equal("internalId", eclReturn.internalId)).futureValue.headOption.value

      setResult     shouldEqual ()
      updatedRecord shouldEqual expectedResult
    }

    "update an existing return with the last updated time set to `now`" in {
      insert(eclReturn).futureValue

      val expectedResult = eclReturn.copy(lastUpdated = Some(now))

      val setResult: Unit = repository.upsert(eclReturn).futureValue
      val updatedRecord   = find(Filters.equal("internalId", eclReturn.internalId)).futureValue.headOption.value

      setResult     shouldEqual ()
      updatedRecord shouldEqual expectedResult
    }
  }

  "get" should {
    "update the lastUpdated time and get the record when there is a record for the id" in {
      insert(eclReturn.copy(lastUpdated = Some(now))).futureValue

      val result         = repository.get(eclReturn.internalId).futureValue
      val expectedResult = eclReturn.copy(lastUpdated = Some(now))

      result.value shouldEqual expectedResult
    }

    "return None when there is no record for the id" in {
      repository.get("id that does not exist").futureValue should not be defined
    }
  }

  "clear" should {
    "remove a record" in {
      insert(eclReturn).futureValue

      val result: Unit = repository.delete(eclReturn.internalId).futureValue

      result                                      shouldEqual ()
      repository.get(eclReturn.internalId).futureValue should not be defined
    }

    "return true when there is no record to remove" in {
      val result: Unit = repository.delete("id that does not exist").futureValue

      result shouldEqual ()
    }
  }

  "keepAlive" should {
    "update lastUpdated to `now` and return true when there is a record for the id" in {
      insert(eclReturn).futureValue

      val result = repository.keepAlive(eclReturn.internalId).futureValue

      val expectedReturn = eclReturn.copy(lastUpdated = Some(now))

      result shouldEqual true
      val updatedReturn = find(Filters.equal("internalId", eclReturn.internalId)).futureValue.headOption.value
      updatedReturn shouldEqual expectedReturn
    }

    "return true when there is no record for the id" in {
      repository.keepAlive("id that does not exist").futureValue shouldEqual true
    }
  }
}
