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
import forms.monthlyreturns.DateConfirmPaymentsFormProvider
import models.ReturnType.MonthlyStandardReturn
import models.ReturnType.MonthlyNilReturn
import models.agent.AgentClientData
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, anyInt, eq as eqTo}
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{DateConfirmPaymentsPage, ReturnTypePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import views.html.monthlyreturns.DateConfirmPaymentsView

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class DateConfirmPaymentsControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"

  private val formProvider = new DateConfirmPaymentsFormProvider(mockFrontendAppConfig)
  private def form         = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  val validAnswer: LocalDate = LocalDate.now(ZoneOffset.UTC)

  lazy val dateConfirmPaymentsRoute: String =
    controllers.monthlyreturns.routes.DateConfirmPaymentsController
      .onPageLoad(NormalMode, Some(MonthlyStandardReturn))
      .url

  override val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, dateConfirmPaymentsRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, dateConfirmPaymentsRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  "DateConfirmPayments Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
        .build()

      running(application) {
        val result = route(application, getRequest).value
        val view   = application.injector.instanceOf[DateConfirmPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, "monthlyreturns.dateConfirmPayments")(
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery when service returns NOT_FOUND" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
        .build()

      running(application) {
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to system error page if unable to retrieve cisId" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
        .build()

      running(application) {
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))

      val userAnswers = UserAnswers(userAnswersId).set(DateConfirmPaymentsPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
        .build()

      running(application) {
        val view   = application.injector.instanceOf[DateConfirmPaymentsView]
        val result = route(application, getRequest).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer),
          NormalMode,
          "monthlyreturns.dateConfirmPayments"
        )(
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(false))
      when(mockMonthlyReturnService.createMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.setOrException(ReturnTypePage, MonthlyStandardReturn)))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers =
        Some(emptyUserAnswers.setOrException(ReturnTypePage, MonthlyStandardReturn))
      ).build()

      val request =
        FakeRequest(POST, dateConfirmPaymentsRoute)
          .withFormUrlEncodedBody("value" -> "invalid value")

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[DateConfirmPaymentsView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, "monthlyreturns.dateConfirmPayments")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest).value

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
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include(
          messages(application)("monthlyreturns.dateConfirmPayments.error.duplicate")
        )
      }
    }

    "must redirect to system error page when duplicate check fails unexpectedly" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must not call duplicate check when invalid data is submitted" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
          .build()

      val request =
        FakeRequest(POST, dateConfirmPaymentsRoute)
          .withFormUrlEncodedBody("value" -> "invalid-value")

      running(application) {
        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
        verifyNoInteractions(mockMonthlyReturnService)
      }
    }

    "must redirect to Journey Recovery on POST when resolveAndStoreCisId returns NOT_FOUND" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must prepare user answers for agent and redirect to next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      val agentClientData          = AgentClientData(
        uniqueId = "CIS-AGENT-123",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        schemeName = Some("Test Employer")
      )

      when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(agentClientData)))
      when(mockMonthlyReturnService.hasClient(eqTo("123"), eqTo("AB456"))(any()))
        .thenReturn(Future.successful(true))
      when(mockMonthlyReturnService.resolveAndStoreCisId(any[UserAnswers], eqTo(true))(any()))
        .thenReturn(Future.successful(("CIS-AGENT-123", emptyUserAnswers)))
      when(mockMonthlyReturnService.isDuplicate(eqTo("CIS-AGENT-123"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(false))
      when(mockMonthlyReturnService.createMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to system error page when agent has no access to this client" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      val agentClientData          = AgentClientData(
        uniqueId = "CIS-AGENT-123",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        schemeName = Some("Test Employer")
      )

      when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(agentClientData)))
      when(mockMonthlyReturnService.hasClient(eqTo("123"), eqTo("AB456"))(any()))
        .thenReturn(Future.successful(false))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to system error page when agent client data is missing" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
