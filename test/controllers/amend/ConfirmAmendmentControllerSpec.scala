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
import models.ReturnType.*
import models.UserAnswers
import models.amend.{AmendmentDetails, CreateAmendedMonthlyReturnRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.{AmendmentDetailsPage, ConfirmAmendmentPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AmendMonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.amend.ConfirmAmendmentView

import scala.concurrent.Future

class ConfirmAmendmentControllerSpec extends SpecBase with MockitoSugar {

  private val handoffId = "handoff-123"

  private val amendmentDetailsStandard = AmendmentDetails(
    instanceId = "1",
    taxYear = 2025,
    taxMonth = 1,
    contractorName = "Test Contractor",
    originalReturnType = MonthlyStandardReturn,
    acceptedTime = Some("2025-04-01T12:00:00Z")
  )

  private val amendmentDetailsNil = amendmentDetailsStandard.copy(originalReturnType = MonthlyNilReturn)

  private def amendmentDetailsWithTime(acceptedTime: Option[String]) =
    amendmentDetailsNil.copy(acceptedTime = acceptedTime)

  "ConfirmAmendment Controller" - {

    "must return OK and the correct view for a GET when handoff exists" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      when(mockSessionRepository.set(any[UserAnswers]())) thenReturn Future.successful(true)

      when(mockAmendMonthlyReturnService.getAmendmentHandoff(any())(any())) thenReturn Future.successful(
        Some(amendmentDetailsStandard)
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
        contentAsString(result) mustEqual view(showWarning = false)(request, messages(application)).toString

        verify(mockAmendMonthlyReturnService).getAmendmentHandoff(eqTo(handoffId))(
          any[HeaderCarrier]()
        )
        verify(mockSessionRepository).set(any[UserAnswers]())
      }
    }

    "must redirect to Journey Recovery for a GET when handoff originalReturnType cannot be amended from" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val unexpectedHandoff =
        amendmentDetailsStandard.copy(originalReturnType = MonthlyAmendedStandardReturn)

      when(
        mockAmendMonthlyReturnService.getAmendmentHandoff(any[String]())(
          any[HeaderCarrier]()
        )
      ) thenReturn Future.successful(Some(unexpectedHandoff))

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

        verify(mockAmendMonthlyReturnService).getAmendmentHandoff(eqTo(handoffId))(
          any[HeaderCarrier]()
        )
        verify(mockSessionRepository, never()).set(any[UserAnswers]())
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

    "must redirect to What Do You Want To Amend Standard when onSubmit succeeds with MonthlyStandardReturn" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val userAnswers =
        emptyUserAnswers
          .set(AmendmentDetailsPage, amendmentDetailsStandard)
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
          controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad().url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(userAnswersCaptor.capture())
        userAnswersCaptor.getValue.get(ConfirmAmendmentPage).value mustEqual true
      }
    }

    "must redirect to What Do You Want To Amend Nil when onSubmit succeeds with MonthlyNilReturn" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val userAnswers =
        emptyUserAnswers
          .set(AmendmentDetailsPage, amendmentDetailsNil)
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
          controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(userAnswersCaptor.capture())
        userAnswersCaptor.getValue.get(ConfirmAmendmentPage).value mustEqual true
      }
    }

    "must redirect to Journey Recovery when create amended monthly return fails" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val userAnswers =
        emptyUserAnswers
          .set(AmendmentDetailsPage, amendmentDetailsStandard)
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

    "must redirect to Journey Recovery when onSubmit succeeds but originalReturnType is unexpected" in {
      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      val unexpectedAmendmentDetails =
        amendmentDetailsStandard.copy(originalReturnType = MonthlyAmendedStandardReturn)

      val userAnswers =
        emptyUserAnswers
          .set(AmendmentDetailsPage, unexpectedAmendmentDetails)
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

        verify(mockAmendMonthlyReturnService).createAmendedMonthlyReturn(
          eqTo(
            CreateAmendedMonthlyReturnRequest(
              instanceId = unexpectedAmendmentDetails.instanceId,
              taxYear = unexpectedAmendmentDetails.taxYear,
              taxMonth = unexpectedAmendmentDetails.taxMonth,
              version = 0
            )
          )
        )(any[HeaderCarrier]())

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(userAnswersCaptor.capture())
        userAnswersCaptor.getValue.get(ConfirmAmendmentPage).value mustEqual true
      }
    }

    "warning period logic" - {

      def buildAppAndRequest(acceptedTime: Option[String]) = {
        val mockSessionRepository         = mock[SessionRepository]
        val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

        when(mockSessionRepository.set(any[UserAnswers]())) thenReturn Future.successful(true)
        when(mockAmendMonthlyReturnService.getAmendmentHandoff(any())(any())) thenReturn Future.successful(
          Some(amendmentDetailsWithTime(acceptedTime))
        )

        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()
      }

      // firstDate = 2016-02-05T00:00:00Z, secondDate = 2016-04-06T00:00:00Z

      "must render view with showWarning = true when acceptedTime is within the warning period" in {
        val application = buildAppAndRequest(Some("2016-04-05T00:00:00Z"))

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.ConfirmAmendmentController.onPageLoad(handoffId).url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ConfirmAmendmentView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(showWarning = true)(request, messages(application)).toString
        }
      }

      "must render view with showWarning = false when acceptedTime equals firstDate (exclusive lower bound)" in {
        val application = buildAppAndRequest(Some("2016-02-05T00:00:00Z"))

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.ConfirmAmendmentController.onPageLoad(handoffId).url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ConfirmAmendmentView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(showWarning = false)(request, messages(application)).toString
        }
      }

      "must render view with showWarning = false when acceptedTime equals secondDate (exclusive upper bound)" in {
        val application = buildAppAndRequest(Some("2016-04-06T00:00:00Z"))

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.ConfirmAmendmentController.onPageLoad(handoffId).url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ConfirmAmendmentView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(showWarning = false)(request, messages(application)).toString
        }
      }

      "must render view with showWarning = false when acceptedTime is before firstDate" in {
        val application = buildAppAndRequest(Some("2016-02-04T00:00:00Z"))

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.ConfirmAmendmentController.onPageLoad(handoffId).url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ConfirmAmendmentView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(showWarning = false)(request, messages(application)).toString
        }
      }

      "must render view with showWarning = false when acceptedTime is after secondDate" in {
        val application = buildAppAndRequest(Some("2016-04-07T00:00:00Z"))

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.ConfirmAmendmentController.onPageLoad(handoffId).url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ConfirmAmendmentView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(showWarning = false)(request, messages(application)).toString
        }
      }

      "must render view with showWarning = false when acceptedTime is None" in {
        val application = buildAppAndRequest(None)

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.ConfirmAmendmentController.onPageLoad(handoffId).url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ConfirmAmendmentView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(showWarning = false)(request, messages(application)).toString
        }
      }
    }
  }
}
