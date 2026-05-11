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
import models.ReturnType.MonthlyStandardReturn
import models.UserAnswers
import models.amend.AmendmentDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.amend.AmendmentDetailsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AmendMonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.amend.ConfirmAmendmentView

import scala.concurrent.Future

class ConfirmAmendmentControllerSpec extends SpecBase {

  private val handoffId = "handoff-123"

  private val amendmentDetails = AmendmentDetails(
    instanceId = "1",
    taxYear = 2025,
    taxMonth = 1,
    originalReturnType = MonthlyStandardReturn,
    acceptedTime = Some("2025-04-01T12:00:00Z")
  )

  "ConfirmAmendment Controller" - {

    "must return OK and the correct view for a GET when handoff exists" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      when(mockSessionRepository.set(any[UserAnswers]())) thenReturn Future.successful(true)

      when(mockAmendMonthlyReturnService.getAmendmentHandoff(any())(any())) thenReturn Future.successful(
        Some(amendmentDetails)
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.amend.routes.ConfirmAmendmentController
              .onPageLoad(handoffId)
              .url
          )

        val result = route(application, request).value
        val view   = application.injector.instanceOf[ConfirmAmendmentView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString

        verify(mockAmendMonthlyReturnService).getAmendmentHandoff(any[String]())(
          any[HeaderCarrier]()
        )
        verify(mockSessionRepository).set(any[UserAnswers]())
      }
    }

    "must redirect to Journey Recovery for a GET when handoff is missing" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      when(
        mockAmendMonthlyReturnService.getAmendmentHandoff(any[String]())(
          any[HeaderCarrier]()
        )
      ) thenReturn Future.successful(None)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.amend.routes.ConfirmAmendmentController
              .onPageLoad(handoffId)
              .url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when onSubmit succeeds" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val userAnswers =
        emptyUserAnswers
          .set(AmendmentDetailsPage, amendmentDetails)
          .success
          .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(
        mockAmendMonthlyReturnService.createAmendedMonthlyReturn(any())(any())
      ) thenReturn Future.successful(())

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.amend.routes.ConfirmAmendmentController
              .onSubmit()
              .url
          ).withFormUrlEncodedBody()

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url

      }
    }

    "must redirect to Journey Recovery when create amended monthly return fails" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val userAnswers =
        emptyUserAnswers
          .set(AmendmentDetailsPage, amendmentDetails)
          .success
          .value

      when(
        mockAmendMonthlyReturnService.createAmendedMonthlyReturn(any())(any())
      ) thenReturn Future.failed(new RuntimeException("failed"))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.amend.routes.ConfirmAmendmentController
              .onSubmit()
              .url
          ).withFormUrlEncodedBody()

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when AmendmentDetails are missing from userAnswers" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.amend.routes.ConfirmAmendmentController
              .onSubmit()
              .url
          ).withFormUrlEncodedBody()

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
