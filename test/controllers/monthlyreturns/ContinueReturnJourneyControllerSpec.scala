package controllers.monthlyreturns

import base.SpecBase
import models.NormalMode
import models.UserAnswers
import models.requests.GetMonthlyReturnForEditRequest
import navigation.Navigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.DateConfirmPaymentsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ContinueReturnJourneyControllerSpec extends SpecBase with MockitoSugar {

  "ContinueReturnJourneyController" - {

    "must redirect to JourneyRecovery when required query params are missing" in {
      val mockService     = mock[MonthlyReturnService]
      val mockNavigator   = mock[Navigator]
      val mockSessionRepo = mock[SessionRepository]

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[Navigator].toInstance(mockNavigator),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.ContinueReturnJourneyController.continueReturnJourney.url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockService)
        verifyNoInteractions(mockNavigator)
        verifyNoInteractions(mockSessionRepo)
      }
    }

    "must save user answers and redirect to next page when population succeeds" in {
      val mockService     = mock[MonthlyReturnService]
      val mockNavigator   = mock[Navigator]
      val mockSessionRepo = mock[SessionRepository]

      val updatedAnswers = UserAnswers("id")
      val nextPage       = controllers.routes.JourneyRecoveryController.onPageLoad()

      when(mockService.populateUserAnswersForContinueJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Right(updatedAnswers)))
      when(mockSessionRepo.set(updatedAnswers))
        .thenReturn(Future.successful(true))
      when(mockNavigator.nextPage(DateConfirmPaymentsPage, NormalMode, updatedAnswers))
        .thenReturn(nextPage)

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[Navigator].toInstance(mockNavigator),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.ContinueReturnJourneyController.continueReturnJourney.url +
            "?instanceId=CIS-123&taxYear=2025&taxMonth=1"
        )

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe nextPage.url

        val requestCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

        verify(mockService).populateUserAnswersForContinueJourney(any[UserAnswers], requestCaptor.capture())(any[HeaderCarrier])
        verify(mockSessionRepo).set(updatedAnswers)
        verify(mockNavigator).nextPage(DateConfirmPaymentsPage, NormalMode, updatedAnswers)

        requestCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 1
        )
      }
    }

    "must redirect to JourneyRecovery when population fails" in {
      val mockService     = mock[MonthlyReturnService]
      val mockNavigator   = mock[Navigator]
      val mockSessionRepo = mock[SessionRepository]

      when(mockService.populateUserAnswersForContinueJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(any[HeaderCarrier])
        ).thenReturn(Future.successful(Left("boom")))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[Navigator].toInstance(mockNavigator),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.ContinueReturnJourneyController.continueReturnJourney.url +
            "?instanceId=CIS-123&taxYear=2025&taxMonth=1"
        )

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        val requestCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

        verify(mockService).populateUserAnswersForContinueJourney(any[UserAnswers], requestCaptor.capture())(any[HeaderCarrier])
        verifyNoInteractions(mockNavigator)
        verifyNoInteractions(mockSessionRepo)

        requestCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 1
        )
      }
    }
  }
}
