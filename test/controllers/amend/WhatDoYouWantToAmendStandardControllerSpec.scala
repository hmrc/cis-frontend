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
import forms.amend.WhatDoYouWantToAmendStandardFormProvider
import models.UserAnswers
import models.amend.WhatDoYouWantToAmendStandard
import models.monthlyreturns.{GetAllMonthlyReturnDetailsResponse, MonthlyReturn, MonthlyReturnItem, Subcontractor}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhatDoYouWantToAmendStandardPage
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import views.html.amend.WhatDoYouWantToAmendStandardView

import java.time.LocalDate
import scala.concurrent.Future

class WhatDoYouWantToAmendStandardControllerSpec extends SpecBase with MockitoSugar {

  lazy val whatDoYouWantToAmendStandardRoute = routes.WhatDoYouWantToAmendStandardController.onPageLoad().url

  val formProvider = new WhatDoYouWantToAmendStandardFormProvider()
  val form         = formProvider()

  val cisId   = "12345678"
  val taxDate = LocalDate.of(2023, 1, 5)

  val baseAnswers = emptyUserAnswers
    .set(CisIdPage, cisId)
    .success
    .value
    .set(DateConfirmPaymentsPage, taxDate)
    .success
    .value

  val mockMonthlyReturn = GetAllMonthlyReturnDetailsResponse(
    scheme = Nil,
    monthlyReturn = Seq(MonthlyReturn(1L, 2023, 1)),
    subcontractors = Seq(
      Subcontractor(
        1L,
        Some("utr"),
        None,
        None,
        None,
        Some("First"),
        None,
        None,
        Some("Last"),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some(100L),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some("First Last")
      )
    ),
    monthlyReturnItems = Seq(
      MonthlyReturnItem(
        1L,
        1L,
        Some("100.00"),
        Some("20.00"),
        Some("10.00"),
        None,
        Some(1L),
        Some("First Last"),
        None,
        Some(100L)
      )
    ),
    submission = Nil
  )

  "WhatDoYouWantToAmendStandard Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendStandardRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatDoYouWantToAmendStandardView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(WhatDoYouWantToAmendStandardPage, WhatDoYouWantToAmendStandard.values.head)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendStandardRoute)

        val view = application.injector.instanceOf[WhatDoYouWantToAmendStandardView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(WhatDoYouWantToAmendStandard.values.head))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Are You Sure You Want To Amend when 'Amend to Nil Return' is submitted" in {

      val mockSessionRepository    = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockMonthlyReturnService.retrieveMonthlyReturnForEditDetails(any())(any())) thenReturn Future.successful(
        mockMonthlyReturn
      )

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendStandardRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendStandard.AmendToNilReturn.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AreYouSureYouWantToAmendYesNoController.onPageLoad().url

        verify(mockMonthlyReturnService).retrieveMonthlyReturnForEditDetails(any())(any())
        verify(mockSessionRepository).set(any())
      }
    }

    "must redirect to Subcontractor Details Added when 'Amend Payment or Subcontractor Details' is submitted" in {

      val mockSessionRepository         = mock[SessionRepository]
      val mockMonthlyReturnService      = mock[MonthlyReturnService]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(
        mockMonthlyReturnService.retrieveMonthlyReturnForEditDetails(any())(any())
      ) thenReturn Future.successful(mockMonthlyReturn)

      when(
        mockAmendMonthlyReturnService.startStandardAmendment(any[UserAnswers]())(any())
      ) thenReturn Future.successful(Right(()))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendStandardRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendStandard.AmendPaymentOrSubcontractorDetails.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
          .onPageLoad(models.NormalMode)
          .url

        verify(mockMonthlyReturnService).retrieveMonthlyReturnForEditDetails(any())(any())
        verify(mockSessionRepository).set(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendStandardRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[WhatDoYouWantToAmendStandardView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendStandardRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendStandardRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendStandard.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
