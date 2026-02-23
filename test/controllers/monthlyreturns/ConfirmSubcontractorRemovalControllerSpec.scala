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
import controllers.routes
import forms.monthlyreturns.ConfirmSubcontractorRemovalFormProvider
import models.monthlyreturns.{DeleteMonthlyReturnItemRequest, SelectedSubcontractor}
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, ConfirmSubcontractorRemovalPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.monthlyreturns.ConfirmSubcontractorRemovalView

import scala.concurrent.Future

class ConfirmSubcontractorRemovalControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new ConfirmSubcontractorRemovalFormProvider()
  private val form         = formProvider()

  private val index             = 1
  private val subcontractorName = "TyneWear Ltd"
  private val subcontractorId   = 123L

  private def routeGet(mode: Mode) =
    controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController.onPageLoad(mode, index).url

  private def routePost(mode: Mode) =
    controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController.onSubmit(mode, index).url

  private def uaWithSubcontractor: UserAnswers =
    emptyUserAnswers
      .set(
        SelectedSubcontractorPage(index),
        SelectedSubcontractor(
          id = subcontractorId,
          name = subcontractorName,
          totalPaymentsMade = Some(BigDecimal(100)),
          costOfMaterials = Some(BigDecimal(50)),
          totalTaxDeducted = Some(BigDecimal(10))
        )
      )
      .success
      .value
      .set(CisIdPage, "abc-123")
      .success
      .value
      .set(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1))
      .success
      .value

  "ConfirmSubcontractorRemoval Controller" - {

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilder(userAnswers = Some(uaWithSubcontractor)).build()

      running(application) {
        val request = FakeRequest(GET, routeGet(NormalMode))

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmSubcontractorRemovalView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, subcontractorName, index)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers =
        uaWithSubcontractor
          .set(ConfirmSubcontractorRemovalPage(index), true)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routeGet(NormalMode))
        val result  = route(application, request).value

        val view = application.injector.instanceOf[ConfirmSubcontractorRemovalView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, subcontractorName, index)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to SubcontractorDetailsAdded when 'No' is submitted and persist the answer" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(uaWithSubcontractor))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routePost(CheckMode))
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode).url

        verify(mockSessionRepository).set(any())
      }
    }

    "must call delete service, remove subcontractor from answers, and redirect to SelectSubcontractors when 'Yes' is submitted" in {
      val mockSessionRepository    = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockMonthlyReturnService.deleteMonthlyReturnItem(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(uaWithSubcontractor))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routePost(CheckMode))
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url

        verify(mockMonthlyReturnService).deleteMonthlyReturnItem(
          eqTo(
            DeleteMonthlyReturnItemRequest(
              instanceId = "abc-123",
              taxYear = 2025,
              taxMonth = 1,
              subcontractorId = subcontractorId
            )
          )
        )(any[HeaderCarrier])

        verify(mockSessionRepository, atLeastOnce()).set(any())
      }
    }

    "must call delete service and redirect to SubcontractorDetailsAdded when 'Yes' is submitted and subcontractors remain" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockMonthlyReturnService.deleteMonthlyReturnItem(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val uaWithTwoSubcontractors =
        uaWithSubcontractor
          .set(
            SelectedSubcontractorPage(2),
            SelectedSubcontractor(
              id = 999L,
              name = "Another Ltd",
              totalPaymentsMade = Some(BigDecimal(200)),
              costOfMaterials = Some(BigDecimal(0)),
              totalTaxDeducted = Some(BigDecimal(20))
            )
          )
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(uaWithTwoSubcontractors))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routePost(CheckMode))
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode).url

        verify(mockMonthlyReturnService).deleteMonthlyReturnItem(any())(any[HeaderCarrier])
        verify(mockSessionRepository, atLeastOnce()).set(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application =
        applicationBuilder(userAnswers = Some(uaWithSubcontractor)).build()

      running(application) {
        val request =
          FakeRequest(POST, routePost(NormalMode))
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ConfirmSubcontractorRemovalView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, subcontractorName, index)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if subcontractor is missing for the index" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routeGet(NormalMode))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if subcontractor is missing for the index" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routePost(NormalMode))
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
