package uk.gov.hmrc.economiccrimelevyreturns.repositories

import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.AdditionalInfo
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class InfoRepositorySpec
    extends AnyWordSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[AdditionalInfo]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val now              = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)
  private val additionalInfo   = AdditionalInfo
    .empty("test-id")
    .copy(
      lastUpdated = Some(Instant.ofEpochSecond(1))
    )
  private val mockAppConfig    = mock[AppConfig]

  when(mockAppConfig.mongoTtl) thenReturn 1

  protected override val repository = new InfoRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  "upsert" should {
    "insert a new return with the last updated time set to `now`" in {
      val expectedResult = additionalInfo.copy(lastUpdated = Some(now))

      val setResult     = repository.upsert(additionalInfo).futureValue
      val updatedRecord = find(Filters.equal("internalId", additionalInfo.internalId)).futureValue.headOption.value

      setResult     shouldEqual true
      updatedRecord shouldEqual expectedResult
    }

    "update an existing return with the last updated time set to `now`" in {
      insert(additionalInfo).futureValue

      val expectedResult = additionalInfo.copy(lastUpdated = Some(now))

      val setResult     = repository.upsert(additionalInfo).futureValue
      val updatedRecord = find(Filters.equal("internalId", additionalInfo.internalId)).futureValue.headOption.value

      setResult     shouldEqual true
      updatedRecord shouldEqual expectedResult
    }
  }

  "get" should {
    "update the lastUpdated time and get the record when there is a record for the id" in {
      insert(additionalInfo.copy(lastUpdated = Some(now))).futureValue

      val result         = repository.get(additionalInfo.internalId).futureValue
      val expectedResult = additionalInfo.copy(lastUpdated = Some(now))

      result.value shouldEqual expectedResult
    }

    "return None when there is no record for the id" in {
      repository.get("id that does not exist").futureValue should not be defined
    }
  }

  "clear" should {
    "remove a record" in {
      insert(additionalInfo).futureValue

      val result = repository.clear(additionalInfo.internalId).futureValue

      result                                           shouldEqual true
      repository.get(additionalInfo.internalId).futureValue should not be defined
    }

    "return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result shouldEqual true
    }
  }

  "keepAlive" should {
    "update lastUpdated to `now` and return true when there is a record for the id" in {
      insert(additionalInfo).futureValue

      val result = repository.keepAlive(additionalInfo.internalId).futureValue

      val expectedReturn = additionalInfo.copy(lastUpdated = Some(now))

      result shouldEqual true
      val updatedReturn = find(Filters.equal("internalId", additionalInfo.internalId)).futureValue.headOption.value
      updatedReturn shouldEqual expectedReturn
    }

    "return true when there is no record for the id" in {
      repository.keepAlive("id that does not exist").futureValue shouldEqual true
    }
  }
}
