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

package controllers.amend

import base.SpecBase
import models.{NormalMode, UserAnswers}
import models.requests.GetMonthlyReturnForEditRequest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{ContinueAmendJourneyResult, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ContinueAmendReturnJourneyControllerSpec extends SpecBase with MockitoSugar {

  private val continueAmendReturnJourneyUrl =
    "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=false"

  private val populatedAnswers = UserAnswers("user-id")

  "ContinueAmendReturnJourneyController" - {

    "must save user answers and redirect to SubcontractorDetailsAdded when subcontractors are present" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = true,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode).url

        verify(mockSessionRepo).set(populatedAnswers)
      }
    }

    "must save user answers and redirect to WhatDoYouWantToAmendNil when isNilReturn and no subcontractors" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = true
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url
      }
    }

    "must save user answers and redirect to WhatDoYouWantToAmendStandard when no subcontractors and not nil" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when service returns Left" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Left("error")))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockSessionRepo)
      }
    }

    "must redirect to WhatDoYouWantToAmendNil when original return is nil and in-progress return is nil" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = true
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=true"
        ).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url
      }
    }

    "must redirect to WhatDoYouWantToAmendStandard when original return is nil but in-progress return is nil" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = true
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=false"
        ).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad().url
      }
    }

    "must pass isAmendment = true to the service" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        route(application, request).value.futureValue

        val captor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

        verify(mockService).populateUserAnswersForContinueAmendJourney(any[UserAnswers], captor.capture())(
          any[HeaderCarrier]
        )

        captor.getValue mustBe GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 1,
          isAmendment = true
        )
      }
    }
  }
}
