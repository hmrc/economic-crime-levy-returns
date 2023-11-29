package uk.gov.hmrc.economiccrimelevyreturns.repositories

import org.mockito.MockitoSugar
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.models.SessionData
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec
    extends AnyWordSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[SessionData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val now              = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)

  private val session = SessionData(
    internalId = "test-id",
    values = Map("email" -> "example@hmrc.com"),
    lastUpdated = Some(Instant.ofEpochSecond(1))
  )

  private val mockAppConfig = mock[AppConfig]

  when(mockAppConfig.mongoTtl) thenReturn 1

  protected override val repository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  "upsert" should {
    "insert a new session with the last updated time set to `now`" in {
      val expectedResult = session.copy(lastUpdated = Some(now))

      val setResult     = repository.upsert(session).futureValue
      val updatedRecord = find(Filters.equal("internalId", session.internalId)).futureValue.headOption.value

      setResult     shouldEqual ()
      updatedRecord shouldEqual expectedResult
    }

    "update an existing session with the last updated time set to `now`" in {
      insert(session).futureValue

      val expectedResult = session.copy(lastUpdated = Some(now))

      val setResult     = repository.upsert(session).futureValue
      val updatedRecord = find(Filters.equal("internalId", session.internalId)).futureValue.headOption.value

      setResult     shouldEqual ()
      updatedRecord shouldEqual expectedResult
    }
  }

  "get" should {
    "update the lastUpdated time and get the record when there is a record for the id" in {
      insert(session.copy(lastUpdated = Some(now))).futureValue

      val result         = repository.get(session.internalId).futureValue
      val expectedResult = session.copy(lastUpdated = Some(now))

      result.value shouldEqual expectedResult
    }

    "return None when there is no record for the id" in {
      repository.get("id that does not exist").futureValue should not be defined
    }
  }

  "deleteRecord" should {
    "remove a record" in {
      insert(session).futureValue

      val result = repository.deleteRecord(session.internalId).futureValue

      result                                    shouldEqual ()
      repository.get(session.internalId).futureValue should not be defined
    }

    "return true when there is no record to remove" in {
      val result = repository.deleteRecord("id that does not exist").futureValue

      result shouldEqual ()
    }
  }

  "keepAlive" should {
    "update lastUpdated to `now` and return true when there is a record for the id" in {
      insert(session).futureValue

      val result = repository.keepAlive(session.internalId).futureValue

      val expectedSession = session.copy(lastUpdated = Some(now))

      result shouldEqual true
      val updatedSession = find(Filters.equal("internalId", session.internalId)).futureValue.headOption.value
      updatedSession shouldEqual expectedSession
    }

    "return true when there is no record for the id" in {
      repository.keepAlive("id that does not exist").futureValue shouldEqual true
    }
  }
}
