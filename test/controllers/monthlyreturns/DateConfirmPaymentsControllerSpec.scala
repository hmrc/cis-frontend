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
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.monthlyreturns.CisTaxpayer
import models.requests.CisPath.CisOrg
import models.{JourneyScope, NormalMode, ReturnType, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyInt, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{DateConfirmPaymentsPage, ReturnTypePage}
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{CisTaxpayerService, MonthlyReturnService}
import views.html.monthlyreturns.DateConfirmPaymentsView

import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.util.UUID
import scala.concurrent.Future

class DateConfirmPaymentsControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val clock: Clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneId.of("Europe/London"))

  when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"

  private val formProvider = new DateConfirmPaymentsFormProvider(mockFrontendAppConfig, clock)
  private def form         = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  val validAnswer: LocalDate = LocalDate.now(clock)

  lazy val dateConfirmPaymentsRoute: String =
    controllers.monthlyreturns.routes.DateConfirmPaymentsController
      .onPageLoad(NormalMode, Some(MonthlyStandardReturn))
      .url

  lazy val dateConfirmPaymentsPostRoute: String =
    controllers.monthlyreturns.routes.DateConfirmPaymentsController
      .onSubmit(NormalMode, MonthlyStandardReturn)
      .url

  lazy val dateConfirmNilPaymentsPostRoute: String =
    controllers.monthlyreturns.routes.DateConfirmPaymentsController
      .onSubmit(NormalMode, MonthlyNilReturn)
      .url

  override val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)
  val standardReturnUserAnswers: UserAnswers =
    userAnswersWithCisId.setOrException(ReturnTypePage, MonthlyStandardReturn)

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, dateConfirmPaymentsRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, dateConfirmPaymentsPostRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  def nilPostRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, dateConfirmNilPaymentsPostRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  private def scopedId(flowId: UUID): String =
    JourneyScope.subJourneyId(CisOrg, flowId)

  private def scopedUserAnswers(flowId: UUID, returnType: ReturnType): UserAnswers = {
    val id = scopedId(flowId)

    emptyUserAnswers
      .initialiseSubJourney(id)
      .success
      .value
      .set(ReturnTypePage(id), returnType)
      .success
      .value
  }

  private def scopedGetRoute(flowId: UUID): String =
    controllers.monthlyreturns.routes.DateConfirmPaymentsController
      .onPageLoadWithId(CisOrg, flowId, NormalMode)
      .url

  private def scopedPostRoute(flowId: UUID): String =
    controllers.monthlyreturns.routes.DateConfirmPaymentsController
      .onSubmitWithId(CisOrg, flowId, NormalMode)
      .url

  private def scopedPostRequest(flowId: UUID): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, scopedPostRoute(flowId))
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  private def scopedApplication(
    userAnswers: Option[UserAnswers],
    monthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService],
    enabled: Boolean = true
  ): (Application, SessionRepository) = {
    val repository = mockSessionRepository(userAnswers)
    when(repository.set(any())).thenReturn(Future.successful(true))

    val taxpayer = mock[CisTaxpayer]
    when(taxpayer.uniqueId).thenReturn("1")

    val taxpayerService = mock[CisTaxpayerService]
    when(taxpayerService.findForOrganisation(using any()))
      .thenReturn(Future.successful(taxpayer))

    val application = applicationBuilder()
      .configure("features.multi-tab-enabled" -> enabled)
      .overrides(
        bind[SessionRepository].toInstance(repository),
        bind[CisTaxpayerService].toInstance(taxpayerService),
        bind[MonthlyReturnService].toInstance(monthlyReturnService)
      )
      .build()

    (application, repository)
  }

  "DateConfirmPayments Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).overrides().build()

      running(application) {
        val result = route(application, getRequest).value
        val view   = application.injector.instanceOf[DateConfirmPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          "monthlyreturns.dateConfirmPayments",
          MonthlyStandardReturn
        )(
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithCisId.set(DateConfirmPaymentsPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).overrides().build()

      running(application) {
        val view   = application.injector.instanceOf[DateConfirmPaymentsView]
        val result = route(application, getRequest).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer),
          NormalMode,
          "monthlyreturns.dateConfirmPayments",
          MonthlyStandardReturn
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
      when(mockMonthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(false))
      when(mockMonthlyReturnService.createMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers =
          Some(userAnswersWithCisId.setOrException(ReturnTypePage, MonthlyStandardReturn))
        )
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
        Some(userAnswersWithCisId.setOrException(ReturnTypePage, MonthlyStandardReturn))
      ).build()

      val request =
        FakeRequest(POST, dateConfirmPaymentsPostRoute)
          .withFormUrlEncodedBody("value" -> "invalid value")

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[DateConfirmPaymentsView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          "monthlyreturns.dateConfirmPayments",
          MonthlyStandardReturn
        )(
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
      when(mockMonthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
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

      when(mockMonthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
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
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
          .build()

      val request =
        FakeRequest(POST, dateConfirmPaymentsPostRoute)
          .withFormUrlEncodedBody("value" -> "invalid-value")

      running(application) {
        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
        verifyNoInteractions(mockMonthlyReturnService)
      }
    }

    "must redirect to next page when StandardReturn and valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockMonthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(false))
      when(mockMonthlyReturnService.createMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(standardReturnUserAnswers), isAgent = true)
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

    "must use existing return type from user answers when returnType parameter is None" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      val userAnswers = userAnswersWithCisId.setOrException(ReturnTypePage, MonthlyStandardReturn)
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
        .build()

      val noReturnTypeRoute =
        controllers.monthlyreturns.routes.DateConfirmPaymentsController.onPageLoad(NormalMode, None).url

      running(application) {
        val request = FakeRequest(GET, noReturnTypeRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[DateConfirmPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          "monthlyreturns.dateConfirmPayments",
          MonthlyStandardReturn
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery when returnType parameter is None and ReturnTypePage is missing" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      val noReturnTypeRoute =
        controllers.monthlyreturns.routes.DateConfirmPaymentsController.onPageLoad(NormalMode, None).url

      running(application) {
        val request = FakeRequest(GET, noReturnTypeRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK with nil return message prefix when returnType is MonthlyNilReturn" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
        .overrides(bind[MonthlyReturnService].toInstance(mockMonthlyReturnService))
        .build()

      val nilReturnRoute =
        controllers.monthlyreturns.routes.DateConfirmPaymentsController
          .onPageLoad(NormalMode, Some(MonthlyNilReturn))
          .url

      running(application) {
        val request = FakeRequest(GET, nilReturnRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[DateConfirmPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          "monthlyreturns.dateConfirmPayments.nilreturn",
          MonthlyNilReturn
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to next page when MonthlyNilReturn and valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
        .thenReturn(Future.successful(false))
      when(mockMonthlyReturnService.createNilMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(emptyUserAnswers))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId.setOrException(ReturnTypePage, MonthlyNilReturn)))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val result = route(application, nilPostRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "UUID-scoped journey" - {

      "must return OK and the correct view for a GET with a valid flowId" in {
        val flowId           = UUID.randomUUID()
        val userAnswers      = scopedUserAnswers(flowId, MonthlyStandardReturn)
        val (application, _) = scopedApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, scopedGetRoute(flowId))
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[DateConfirmPaymentsView]

          val formAction = Some(
            controllers.monthlyreturns.routes.DateConfirmPaymentsController
              .onSubmitWithId(CisOrg, flowId, NormalMode)
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            form,
            NormalMode,
            "monthlyreturns.dateConfirmPayments",
            MonthlyStandardReturn,
            formAction
          )(request, messages(application)).toString
        }
      }

      "must populate the form from the correct scoped answers" in {
        val flowId = UUID.randomUUID()
        val id     = scopedId(flowId)

        val userAnswers = scopedUserAnswers(flowId, MonthlyStandardReturn)
          .set(DateConfirmPaymentsPage(id), validAnswer)
          .success
          .value

        val (application, _) = scopedApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, scopedGetRoute(flowId))
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[DateConfirmPaymentsView]

          val formAction = Some(
            controllers.monthlyreturns.routes.DateConfirmPaymentsController
              .onSubmitWithId(CisOrg, flowId, NormalMode)
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            form.fill(validAnswer),
            NormalMode,
            "monthlyreturns.dateConfirmPayments",
            MonthlyStandardReturn,
            formAction
          )(request, messages(application)).toString
        }
      }

      "must return OK with nil return messages for a nil journey" in {
        val flowId           = UUID.randomUUID()
        val userAnswers      = scopedUserAnswers(flowId, MonthlyNilReturn)
        val (application, _) = scopedApplication(Some(userAnswers))

        running(application) {
          val request = FakeRequest(GET, scopedGetRoute(flowId))
          val result  = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) must include(
            messages(application)("monthlyreturns.dateConfirmPayments.nilreturn.title")
          )
        }
      }

      "must redirect to Journey Recovery when the flowId does not exist" in {
        val flowId           = UUID.randomUUID()
        val (application, _) = scopedApplication(Some(emptyUserAnswers))

        running(application) {
          val result = route(application, FakeRequest(GET, scopedGetRoute(flowId))).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped ReturnTypePage is missing" in {
        val flowId = UUID.randomUUID()

        val userAnswers = emptyUserAnswers
          .initialiseSubJourney(scopedId(flowId))
          .success
          .value

        val (application, _) = scopedApplication(Some(userAnswers))

        running(application) {
          val result = route(application, FakeRequest(GET, scopedGetRoute(flowId))).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must save the scoped date and create a standard return" in {
        val flowId      = UUID.randomUUID()
        val id          = scopedId(flowId)
        val userAnswers = scopedUserAnswers(flowId, MonthlyStandardReturn)

        val monthlyReturnService = mock[MonthlyReturnService]

        when(monthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
          .thenReturn(Future.successful(false))

        when(monthlyReturnService.createMonthlyReturn(any())(any()))
          .thenReturn(Future.successful(()))

        val (application, repository) =
          scopedApplication(Some(userAnswers), monthlyReturnService)

        running(application) {
          val result = route(application, scopedPostRequest(flowId)).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe scopedGetRoute(flowId)

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository).set(captor.capture())

          captor.getValue.get(DateConfirmPaymentsPage(id)) mustBe Some(
            LocalDate.of(2026, 7, 5)
          )
          captor.getValue.get(DateConfirmPaymentsPage) mustBe None
        }
      }

      "must save the scoped date and create a nil return" in {
        val flowId      = UUID.randomUUID()
        val id          = scopedId(flowId)
        val userAnswers = scopedUserAnswers(flowId, MonthlyNilReturn)

        val monthlyReturnService = mock[MonthlyReturnService]

        when(monthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
          .thenReturn(Future.successful(false))

        when(monthlyReturnService.createNilMonthlyReturn(any[UserAnswers], eqTo("1"), eqTo(id))(any()))
          .thenReturn(Future.successful(userAnswers))

        val (application, repository) =
          scopedApplication(Some(userAnswers), monthlyReturnService)

        running(application) {
          val result = route(application, scopedPostRequest(flowId)).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe scopedGetRoute(flowId)

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository).set(captor.capture())

          captor.getValue.get(DateConfirmPaymentsPage(id)) mustBe Some(
            LocalDate.of(2026, 7, 5)
          )

          verify(monthlyReturnService)
            .createNilMonthlyReturn(any[UserAnswers], eqTo("1"), eqTo(id))(any())
        }
      }

      "must return Bad Request when invalid data is submitted" in {
        val flowId               = UUID.randomUUID()
        val userAnswers          = scopedUserAnswers(flowId, MonthlyStandardReturn)
        val monthlyReturnService = mock[MonthlyReturnService]

        val (application, repository) =
          scopedApplication(Some(userAnswers), monthlyReturnService)

        val request = FakeRequest(POST, scopedPostRoute(flowId))
          .withFormUrlEncodedBody("value" -> "invalid-value")

        running(application) {
          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST

          verifyNoInteractions(monthlyReturnService)
        }
      }

      "must return a duplicate error without saving the scoped date" in {
        val flowId               = UUID.randomUUID()
        val userAnswers          = scopedUserAnswers(flowId, MonthlyStandardReturn)
        val monthlyReturnService = mock[MonthlyReturnService]

        when(monthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
          .thenReturn(Future.successful(true))

        val (application, repository) =
          scopedApplication(Some(userAnswers), monthlyReturnService)

        running(application) {
          val result = route(application, scopedPostRequest(flowId)).value

          status(result) mustBe BAD_REQUEST
          contentAsString(result) must include(
            messages(application)("monthlyreturns.dateConfirmPayments.error.duplicate")
          )
        }
      }

      "must redirect to System Error when the duplicate check fails" in {
        val flowId               = UUID.randomUUID()
        val userAnswers          = scopedUserAnswers(flowId, MonthlyStandardReturn)
        val monthlyReturnService = mock[MonthlyReturnService]

        when(monthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
          .thenReturn(Future.failed(new RuntimeException("Backend error")))

        val (application, _) =
          scopedApplication(Some(userAnswers), monthlyReturnService)

        running(application) {
          val result = route(application, scopedPostRequest(flowId)).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            routes.SystemErrorController.onPageLoad().url
        }
      }

      "must redirect an existing journey when multi-tab is disabled" in {
        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId, MonthlyStandardReturn)

        val (application, _) =
          scopedApplication(Some(userAnswers), enabled = false)

        running(application) {
          val result = route(application, FakeRequest(GET, scopedGetRoute(flowId))).value

          val expectedUrl = application.injector
            .instanceOf[FrontendAppConfig]
            .returnsLandingPageUrl("1", None)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe expectedUrl
        }
      }

      "must not overwrite answers belonging to another flowId" in {
        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()

        val firstId  = scopedId(firstFlowId)
        val secondId = scopedId(secondFlowId)

        val secondDate = LocalDate.of(2025, 3, 5)

        val userAnswers = scopedUserAnswers(firstFlowId, MonthlyStandardReturn)
          .initialiseSubJourney(secondId)
          .success
          .value
          .set(ReturnTypePage(secondId), MonthlyStandardReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage(secondId), secondDate)
          .success
          .value

        val monthlyReturnService = mock[MonthlyReturnService]

        when(monthlyReturnService.isDuplicate(eqTo("1"), anyInt(), anyInt())(any()))
          .thenReturn(Future.successful(false))

        when(monthlyReturnService.createMonthlyReturn(any())(any()))
          .thenReturn(Future.successful(()))

        val (application, repository) =
          scopedApplication(Some(userAnswers), monthlyReturnService)

        running(application) {
          val result = route(application, scopedPostRequest(firstFlowId)).value

          status(result) mustBe SEE_OTHER

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository).set(captor.capture())

          val savedAnswers = captor.getValue

          savedAnswers.get(DateConfirmPaymentsPage(firstId)) mustBe Some(validAnswer.withDayOfMonth(5))
          savedAnswers.get(DateConfirmPaymentsPage(secondId)) mustBe Some(secondDate)
        }
      }
    }
  }
}
