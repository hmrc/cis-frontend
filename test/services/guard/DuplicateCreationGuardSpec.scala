package services.guard

import models.UserAnswers
import models.monthlyreturns.MonthlyReturnEntity
import models.requests.DataRequest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pages.monthlyreturns.MonthlyReturnEntityPage
import play.api.test.FakeRequest
import services.guard.DuplicateCreationCheck.DuplicateFound

import java.time.LocalDateTime
import scala.concurrent.Future

class DuplicateCreationGuardSpec extends AnyWordSpec with Matchers with ScalaFutures{
  private val guard = new DuplicateCreationGuardImpl

  private def emptyUserAnswers(userId: String = "uid"): UserAnswers =
    UserAnswers(userId)

  private def dataRequest(ua: UserAnswers): DataRequest[_] =
    DataRequest(
      request = FakeRequest(),
      userId = ua.id,
      userAnswers = ua,
      employerReference = None,
      isAgent = false
    )

  private val now = LocalDateTime.now
  private val exampleEntity = MonthlyReturnEntity(
    monthlyReturnId = 0L,
    schemeId = 0L,
    taxYear = 2025,
    taxMonth = 5,
    taxYearPrevious = None,
    taxMonthPrevious = None,
    nilReturnIndicator = None,
    decNilReturnNoPayments = None,
    decInformationCorrect = None,
    decNoMoreSubPayments = None,
    decAllSubsVerified = None,
    decEmpStatusConsidered = None,
    status = None,
    createDate = now,
    lastUpdate = now,
    version = 1,
    lMigrated = None,
    amendment = None,
    supersededBy = None
  )

  "DuplicateCreationGuardImpl.check" should {

    "return NoDuplicate when MonthlyReturnEntityPage is present" in {
      val ua = emptyUserAnswers()
      implicit val request: DataRequest[_] = dataRequest(ua)

      val result: Future[DuplicateCreationCheck] = guard.check
      result.futureValue mustBe DuplicateCreationCheck.NoDuplicate
    }

    "return DuplicateFound when MonthlyReturnEntityPage is present" in {
      val uaWithEntity = emptyUserAnswers().set(MonthlyReturnEntityPage, exampleEntity).get
      implicit val request: DataRequest[_] = dataRequest(uaWithEntity)

      val result = guard.check
      result.futureValue mustBe DuplicateFound
    }

  }
  
}
