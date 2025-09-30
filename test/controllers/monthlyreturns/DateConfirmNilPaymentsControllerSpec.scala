/*
 * Copyright 2025 HM Revenue & Customs
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
import config.FrontendAppConfig
import controllers.routes
import forms.monthlyreturns.DateConfirmNilPaymentsFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, anyInt, eq as eqTo}
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.DateConfirmNilPaymentsPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.monthlyreturns.DateConfirmNilPaymentsView

import java.time.format.TextStyle
import java.time.{LocalDate, Month, ZoneOffset}
import java.util.Locale
import scala.concurrent.Future

class DateConfirmNilPaymentsControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"

  private val formProvider = new DateConfirmNilPaymentsFormProvider(mockFrontendAppConfig)
  private def form         = formProvider()

  def onwardRoute = Call("GET", "/foo")

  val validAnswer = LocalDate.now(ZoneOffset.UTC)

  lazy val dateConfirmNilPaymentsRoute =
    controllers.monthlyreturns.routes.DateConfirmNilPaymentsController.onPageLoad(NormalMode).url

  override val emptyUserAnswers = UserAnswers(userAnswersId)

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, dateConfirmNilPaymentsRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, dateConfirmNilPaymentsRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  "DateConfirmNilPayments Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val result = route(application, getRequest()).value
        val view   = application.injector.instanceOf[DateConfirmNilPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(getRequest(), messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when service return NOT_FOUND" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val result = route(application, getRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return 500 and show global error when service fails with non-NOT_FOUND" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val result = route(application, getRequest()).value
        val view   = application.injector.instanceOf[DateConfirmNilPaymentsView]

        val errorForm = form.withGlobalError("monthlyreturns.dateConfirmNilPayments.error.technical")

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) mustEqual view(errorForm, NormalMode)(getRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))

      val userAnswers = UserAnswers(userAnswersId).set(DateConfirmNilPaymentsPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val view   = application.injector.instanceOf[DateConfirmNilPaymentsView]
        val result = route(application, getRequest()).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode)(
          getRequest(),
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(false))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, dateConfirmNilPaymentsRoute)
            .withFormUrlEncodedBody(
              "value.month" -> validAnswer.getMonthValue.toString,
              "value.year"  -> validAnswer.getYear.toString
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request =
        FakeRequest(POST, dateConfirmNilPaymentsRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[DateConfirmNilPaymentsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return Bad Request with duplicate error when the submitted month/year already exists" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, dateConfirmNilPaymentsRoute)
            .withFormUrlEncodedBody(
              "value.month" -> validAnswer.getMonthValue.toString,
              "value.year"  -> validAnswer.getYear.toString
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val monthName   =
          Month.of(validAnswer.getMonthValue).getDisplayName(TextStyle.FULL, Locale.UK)
        val expectedMsg =
          messages(application)(
            "monthlyreturns.dateConfirmNilPayments.error.duplicate",
            monthName,
            validAnswer.getYear.toString
          )

        contentAsString(result) must include(expectedMsg)
      }
    }

    "must return Internal Server Error when duplicate check fails unexpectedly" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, dateConfirmNilPaymentsRoute)
            .withFormUrlEncodedBody(
              "value.month" -> validAnswer.getMonthValue.toString,
              "value.year"  -> validAnswer.getYear.toString
            )

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) must include(
          messages(application)("monthlyreturns.dateConfirmNilPayments.error.technical")
        )
      }
    }

    "must not call duplicate check when invalid data is submitted" in {

      val mockMonthlyReturnService = mock[MonthlyReturnService]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      val request =
        FakeRequest(POST, dateConfirmNilPaymentsRoute)
          .withFormUrlEncodedBody(
            "value" -> "invalid-value"
          )

      running(application) {
        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
        verifyNoInteractions(mockMonthlyReturnService)
      }
    }

  }
}
