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
import config.FrontendAppConfig
import controllers.routes
import forms.monthlyreturns.EnterYourEmailAddressFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, ConfirmationByEmailPage, EnterYourEmailAddressPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import views.html.monthlyreturns.EnterYourEmailAddressView

import java.util.UUID
import scala.concurrent.Future

class EnterYourEmailAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new EnterYourEmailAddressFormProvider()
  val form         = formProvider()

  lazy val enterYourEmailAddressRoute =
    controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(NormalMode).url

  val userAnswersForEmailPage = userAnswersWithCisId.set(ConfirmationByEmailPage, true).success.value

  private def scopedUserAnswers(flowId: UUID): UserAnswers = {
    val id = flowId.toString

    emptyUserAnswers
      .setOrException(CisIdPage(id), "test-cis-id")
      .setOrException(ConfirmationByEmailPage(id), true)
  }

  "EnterYourEmailAddress Controller" - {

    "must call getSchemeEmail and prepopulate the email address for a GET in NormalMode when not already answered" in {

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
        .thenReturn(Future.successful(Some("prepopulated@test.com")))

      val application = applicationBuilder(userAnswers = Some(userAnswersForEmailPage))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("prepopulated@test.com"), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must call getSchemeEmail and render an empty form when the service returns None" in {

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = Some(userAnswersForEmailPage))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must call getSchemeEmail and prepopulate the email address for a GET in CheckMode when not already answered" in {

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
        .thenReturn(Future.successful(Some("prepopulated@test.com")))

      val application = applicationBuilder(userAnswers = Some(userAnswersForEmailPage))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val checkModeRoute =
          controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(models.CheckMode).url
        val request        = FakeRequest(GET, checkModeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("prepopulated@test.com"), models.CheckMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must gracefully recover and render the page with an empty form if getSchemeEmail fails" in {

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("backend error")))

      val application = applicationBuilder(userAnswers = Some(userAnswersForEmailPage))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersForEmailPage.set(EnterYourEmailAddressPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", "test@example.com"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to ConfirmationByEmail page on a GET when ConfirmationByEmailPage is not answered with true" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "UUID-scoped journey" - {

      "must call getSchemeEmail and prepopulate the email address for a GET in NormalMode when not already answered" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val mockMonthlyReturnService = mock[MonthlyReturnService]
        when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
          .thenReturn(Future.successful(Some("prepopulated@test.com")))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val formAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("prepopulated@test.com"), NormalMode, formAction)(
            request,
            messages(application)
          ).toString

          verify(mockMonthlyReturnService).getSchemeEmail(any())(any())
        }
      }

      "must call getSchemeEmail and render an empty form when the service returns None" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val mockMonthlyReturnService = mock[MonthlyReturnService]
        when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val formAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must call getSchemeEmail and prepopulate the email address for a GET in CheckMode when not already answered" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val mockMonthlyReturnService = mock[MonthlyReturnService]
        when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
          .thenReturn(Future.successful(Some("prepopulated@test.com")))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val checkModeRoute =
            controllers.monthlyreturns.routes.EnterYourEmailAddressController
              .onPageLoadWithId(flowId, models.CheckMode)
              .url
          val request        = FakeRequest(GET, checkModeRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val formAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, models.CheckMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("prepopulated@test.com"), models.CheckMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must gracefully recover and render the page with an empty form if getSchemeEmail fails" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val mockMonthlyReturnService = mock[MonthlyReturnService]
        when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("backend error")))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val formAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the scoped question has previously been answered" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = scopedUserAnswers(flowId)
          .set(EnterYourEmailAddressPage(id), "answer@test.com")
          .success
          .value

        val mockMonthlyReturnService = mock[MonthlyReturnService]

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val result = route(application, request).value

          val formAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer@test.com"), NormalMode, formAction)(
            request,
            messages(application)
          ).toString

          verifyNoInteractions(mockMonthlyReturnService)
        }
      }

      "must populate the view using the email address from the correct UUID-scoped journey" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()
        val firstId      = firstFlowId.toString
        val secondId     = secondFlowId.toString

        val userAnswers = scopedUserAnswers(firstFlowId)
          .set(EnterYourEmailAddressPage(firstId), "first@example.com")
          .success
          .value
          .set(CisIdPage(secondId), "second-cis-id")
          .success
          .value
          .set(ConfirmationByEmailPage(secondId), true)
          .success
          .value
          .set(EnterYourEmailAddressPage(secondId), "second@example.com")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val firstRequest = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController
              .onPageLoadWithId(firstFlowId, NormalMode)
              .url
          )

          val secondRequest = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController
              .onPageLoadWithId(secondFlowId, NormalMode)
              .url
          )

          val firstResult  = route(application, firstRequest).value
          val secondResult = route(application, secondRequest).value

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val firstFormAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(firstFlowId, NormalMode)
          )

          val secondFormAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(secondFlowId, NormalMode)
          )

          status(firstResult) mustEqual OK
          contentAsString(firstResult) mustEqual view(form.fill("first@example.com"), NormalMode, firstFormAction)(
            firstRequest,
            messages(application)
          ).toString

          status(secondResult) mustEqual OK
          contentAsString(secondResult) mustEqual view(form.fill("second@example.com"), NormalMode, secondFormAction)(
            secondRequest,
            messages(application)
          ).toString
        }
      }

      "must redirect to ConfirmationByEmail page on a GET when the scoped ConfirmationByEmailPage is false" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = scopedUserAnswers(flowId)
          .set(ConfirmationByEmailPage(id), false)
          .success
          .value

        val mockMonthlyReturnService = mock[MonthlyReturnService]

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(flowId, NormalMode).url

          verifyNoInteractions(mockMonthlyReturnService)
        }
      }

      "must redirect to ConfirmationByEmail page on a GET when the scoped ConfirmationByEmailPage is missing" in {

        val flowId = UUID.randomUUID()

        val userAnswers = emptyUserAnswers
          .set(CisIdPage(flowId.toString), "test-cis-id")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(flowId, NormalMode).url
        }
      }

      "must save the scoped email address and redirect to Declaration when valid data is submitted" in {

        val flowId      = UUID.randomUUID()
        val id          = flowId.toString
        val userAnswers = scopedUserAnswers(flowId)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", "test@example.com"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(flowId).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          captor.getValue.get(EnterYourEmailAddressPage(id)) mustEqual Some("test@example.com")
          captor.getValue.get(EnterYourEmailAddressPage) mustEqual None
        }
      }

      "must save the scoped email address and redirect to Check Your Answers in CheckMode" in {

        val flowId      = UUID.randomUUID()
        val id          = flowId.toString
        val userAnswers = scopedUserAnswers(flowId)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.EnterYourEmailAddressController
                .onSubmitWithId(flowId, models.CheckMode)
                .url
            )
              .withFormUrlEncodedBody(("value", "test@example.com"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          captor.getValue.get(EnterYourEmailAddressPage(id)) mustEqual Some("test@example.com")
        }
      }

      "must save the email address for the correct UUID without changing another journey" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()
        val firstId      = firstFlowId.toString
        val secondId     = secondFlowId.toString

        val userAnswers = scopedUserAnswers(firstFlowId)
          .set(CisIdPage(secondId), "second-cis-id")
          .success
          .value
          .set(ConfirmationByEmailPage(secondId), true)
          .success
          .value
          .set(EnterYourEmailAddressPage(secondId), "second@example.com")
          .success
          .value

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.EnterYourEmailAddressController
                .onSubmitWithId(firstFlowId, NormalMode)
                .url
            )
              .withFormUrlEncodedBody(("value", "first@example.com"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(firstFlowId).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          val savedAnswers = captor.getValue

          savedAnswers.get(EnterYourEmailAddressPage(firstId)) mustEqual Some("first@example.com")
          savedAnswers.get(EnterYourEmailAddressPage(secondId)) mustEqual Some("second@example.com")
          savedAnswers.get(CisIdPage(secondId)) mustEqual Some("second-cis-id")
        }
      }

      "must return a Bad Request and errors when invalid scoped data is submitted" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[EnterYourEmailAddressView]

          val result = route(application, request).value

          val formAction = Some(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = None)
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = None)
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.EnterYourEmailAddressController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", "test@example.com"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped CisIdPage is missing" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = Some(userAnswersForEmailPage))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the returns landing page when multi-tab is disabled" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.returnsLandingPageUrl("test-cis-id", None)
        }
      }
    }
  }
}
