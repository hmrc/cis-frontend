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
import config.FrontendAppConfig
import forms.amend.ConfirmCancelAmendmentYesNoFormProvider
import models.NormalMode
import models.ReturnType.MonthlyAmendedStandardReturn
import models.amend.DeleteUnsubmittedMonthlyReturnRequest
import models.monthlyreturns.{MonthlyReturnDetails, MonthlyReturnResponse}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ConfirmCancelAmendmentYesNoPage
import pages.monthlyreturns.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.amend.ConfirmCancelAmendmentYesNoView

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmCancelAmendmentYesNoControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new ConfirmCancelAmendmentYesNoFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val confirmCancelAmendmentYesNoRoute: String =
    routes.ConfirmCancelAmendmentYesNoController.onPageLoad().url

  private val monthYear: String = "April 2026"
  private val cisId: String     = "1"

  private val userAnswersWithDate = emptyUserAnswers
    .set(CisIdPage, cisId)
    .success
    .value
    .set(DateConfirmPaymentsPage, LocalDate.of(2026, 4, 1))
    .success
    .value

  private val userAnswersWithCisIdOnly = emptyUserAnswers
    .set(CisIdPage, cisId)
    .success
    .value

  private val cancellableMonthlyReturnResponse =
    MonthlyReturnResponse(
      monthlyReturnList = Seq(
        MonthlyReturnDetails(
          monthlyReturnId = 1L,
          taxYear = 2026,
          taxMonth = 4,
          nilReturnIndicator = None,
          decEmpStatusConsidered = None,
          decAllSubsVerified = None,
          decInformationCorrect = None,
          decNoMoreSubPayments = None,
          decNilReturnNoPayments = None,
          status = Some("STARTED"),
          lastUpdate = None,
          amendment = Some("Y"),
          supersededBy = None
        )
      )
    )

  private def monthlyReturnServiceMock(
    response: MonthlyReturnResponse = cancellableMonthlyReturnResponse
  ): MonthlyReturnService = {
    val service = mock[MonthlyReturnService]

    when(service.retrieveAllMonthlyReturns(any[String]())(using any[HeaderCarrier]))
      .thenReturn(Future.successful(response))

    service
  }

  "ConfirmCancelAmendmentYesNo Controller" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDate))
          .overrides(
            bind[MonthlyReturnService].toInstance(monthlyReturnServiceMock())
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, confirmCancelAmendmentYesNoRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmCancelAmendmentYesNoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, monthYear)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithDate
        .set(ConfirmCancelAmendmentYesNoPage, true)
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[MonthlyReturnService].toInstance(monthlyReturnServiceMock())
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, confirmCancelAmendmentYesNoRoute)

        val view = application.injector.instanceOf[ConfirmCancelAmendmentYesNoView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), monthYear)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDate))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnServiceMock())
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmCancelAmendmentYesNoRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode).url
      }
    }

    "must delete unsubmitted monthly return when Yes is submitted" in {
      val mockSessionRepository   = mock[SessionRepository]
      val mockAmendMonthlyService = mock[AmendMonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(
        mockAmendMonthlyService.deleteUnsubmittedMonthlyReturn(any[DeleteUnsubmittedMonthlyReturnRequest]())(
          any()
        )
      ) thenReturn Future.successful(())

      val userAnswers = emptyUserAnswers
        .set(CisIdPage, cisId)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 4, 1))
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyService),
            bind[MonthlyReturnService].toInstance(monthlyReturnServiceMock())
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmCancelAmendmentYesNoRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appConfig.returnsLandingPageUrl(cisId, None)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDate))
          .overrides(
            bind[MonthlyReturnService].toInstance(monthlyReturnServiceMock())
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmCancelAmendmentYesNoRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ConfirmCancelAmendmentYesNoView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, monthYear)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, confirmCancelAmendmentYesNoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmCancelAmendmentYesNoRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when monthYear is missing" in {
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisIdOnly)).build()

      running(application) {
        val request = FakeRequest(GET, confirmCancelAmendmentYesNoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when monthYear is missing" in {
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisIdOnly)).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmCancelAmendmentYesNoRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when monthly return status is not STARTED or VALIDATED" in {

      val nonCancellableMonthlyReturnResponse =
        cancellableMonthlyReturnResponse.copy(
          monthlyReturnList = Seq(
            cancellableMonthlyReturnResponse.monthlyReturnList.head.copy(
              status = Some("SUBMITTED")
            )
          )
        )

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDate))
          .overrides(
            bind[MonthlyReturnService].toInstance(
              monthlyReturnServiceMock(nonCancellableMonthlyReturnResponse)
            )
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, confirmCancelAmendmentYesNoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when monthly return is not an amendment" in {

      val nonAmendmentMonthlyReturnResponse =
        cancellableMonthlyReturnResponse.copy(
          monthlyReturnList = Seq(
            cancellableMonthlyReturnResponse.monthlyReturnList.head.copy(
              amendment = Some("N")
            )
          )
        )

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDate))
          .overrides(
            bind[MonthlyReturnService].toInstance(
              monthlyReturnServiceMock(nonAmendmentMonthlyReturnResponse)
            )
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, confirmCancelAmendmentYesNoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
