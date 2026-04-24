/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.monthlyreturns

import base.SpecBase
import models.NormalMode
import models.UserAnswers
import models.monthlyreturns.ContinueReturnJourneyQueryParams
import models.requests.GetMonthlyReturnForEditRequest
import navigation.Navigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ContinueReturnJourneyControllerSpec extends SpecBase with MockitoSugar {

  private val queryParams = ContinueReturnJourneyQueryParams(
    instanceId = "CIS-123",
    taxYear = 2025,
    taxMonth = 1
  )

  private val continueReturnJourneyUrl =
    controllers.monthlyreturns.routes.ContinueReturnJourneyController
      .continueReturnJourney(queryParams)
      .url

  "ContinueReturnJourneyController" - {

    "must save final user answers and redirect to next page when population succeeds" in {
      val mockService     = mock[MonthlyReturnService]
      val mockNavigator   = mock[Navigator]
      val mockSessionRepo = mock[SessionRepository]

      val updatedAnswers = UserAnswers("id")
      val finalAnswers   = updatedAnswers.set(CisIdPage, "CIS-123").success.value
      val nextPage       = controllers.routes.JourneyRecoveryController.onPageLoad()

      when(
        mockService.populateUserAnswersForContinueJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(updatedAnswers)))

      when(
        mockService.populateAgentClientDataIfRequired(
          ua = any[UserAnswers],
          userId = any[String],
          isAgent = any[Boolean]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(finalAnswers))

      when(mockSessionRepo.set(finalAnswers))
        .thenReturn(Future.successful(true))

      when(mockNavigator.nextPage(DateConfirmPaymentsPage, NormalMode, finalAnswers))
        .thenReturn(nextPage)

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[Navigator].toInstance(mockNavigator),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe nextPage.url

        val requestCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

        verify(mockService).populateUserAnswersForContinueJourney(any[UserAnswers], requestCaptor.capture())(
          any[HeaderCarrier]
        )

        verify(mockService).populateAgentClientDataIfRequired(
          ua = any[UserAnswers],
          userId = any[String],
          isAgent = any[Boolean]
        )(any[HeaderCarrier])

        verify(mockSessionRepo).set(finalAnswers)
        verify(mockNavigator).nextPage(DateConfirmPaymentsPage, NormalMode, finalAnswers)

        requestCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 1
        )
      }
    }

    "must redirect to JourneyRecovery when initial population fails" in {
      val mockService     = mock[MonthlyReturnService]
      val mockNavigator   = mock[Navigator]
      val mockSessionRepo = mock[SessionRepository]

      when(
        mockService.populateUserAnswersForContinueJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Left("boom")))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[Navigator].toInstance(mockNavigator),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        val requestCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

        verify(mockService).populateUserAnswersForContinueJourney(any[UserAnswers], requestCaptor.capture())(
          any[HeaderCarrier]
        )

        verify(mockService, never()).populateAgentClientDataIfRequired(
          ua = any[UserAnswers],
          userId = any[String],
          isAgent = any[Boolean]
        )(any[HeaderCarrier])

        verifyNoInteractions(mockNavigator)
        verifyNoInteractions(mockSessionRepo)

        requestCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 1
        )
      }
    }

    "must redirect to JourneyRecovery when agent client data population fails" in {
      val mockService     = mock[MonthlyReturnService]
      val mockNavigator   = mock[Navigator]
      val mockSessionRepo = mock[SessionRepository]

      val updatedAnswers = UserAnswers("id")

      when(
        mockService.populateUserAnswersForContinueJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(updatedAnswers)))

      when(
        mockService.populateAgentClientDataIfRequired(
          ua = any[UserAnswers],
          userId = any[String],
          isAgent = any[Boolean]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[Navigator].toInstance(mockNavigator),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService)
          .populateUserAnswersForContinueJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
            any[HeaderCarrier]
          )

        verify(mockService).populateAgentClientDataIfRequired(
          ua = any[UserAnswers],
          userId = any[String],
          isAgent = any[Boolean]
        )(any[HeaderCarrier])

        verifyNoInteractions(mockNavigator)
        verifyNoInteractions(mockSessionRepo)
      }
    }
  }
}
