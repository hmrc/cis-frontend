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
import models.monthlyreturns.Declaration
import models.{NormalMode, UserAnswers}
import models.ReturnType.MonthlyNilReturn
import navigation.Navigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.DeclarationView

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val declarationRoute: String = routes.DeclarationController.onPageLoad().url
  lazy val submitRoute: String      = routes.DeclarationController.onSubmit().url

  private def scopedUserAnswers(flowId: UUID): UserAnswers =
    emptyUserAnswers
      .setOrException(CisIdPage(flowId.toString), "test-cis-id")
      .setOrException(ReturnTypePage(flowId.toString), MonthlyNilReturn)

  "Declaration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeclarationView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(
          result
        ).value mustEqual controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCisId.set(DeclarationPage, Set(Declaration.Confirmed)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val view = application.injector.instanceOf[DeclarationView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when date is available" in {

      val testDate    = LocalDate.of(2024, 4, 5)
      val userAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, testDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val view = application.injector.instanceOf[DeclarationView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when the button is submitted (GET)" in {
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[Navigator].toInstance(new navigation.FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, submitRoute)
            .withFormUrlEncodedBody()

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "UUID-scoped journey" - {

      "must return OK and the correct view for a GET with a valid flowId" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.DeclarationController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[DeclarationView]

          val continueUrl = Some(routes.DeclarationController.onSubmitWithId(flowId).url)

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(NormalMode, continueUrl)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET when the scoped declaration has previously been answered" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = scopedUserAnswers(flowId)
          .set(DeclarationPage(id), Set(Declaration.Confirmed))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.DeclarationController.onPageLoadWithId(flowId).url
          )

          val view = application.injector.instanceOf[DeclarationView]

          val result = route(application, request).value

          val continueUrl = Some(routes.DeclarationController.onSubmitWithId(flowId).url)

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(NormalMode, continueUrl)(
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
            routes.DeclarationController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped CisIdPage is missing" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.DeclarationController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped CisIdPage is blank" in {

        val flowId = UUID.randomUUID()

        val userAnswers = emptyUserAnswers
          .set(CisIdPage(flowId.toString), " ")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.DeclarationController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
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
            routes.DeclarationController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.returnsLandingPageUrl("test-cis-id", None)
        }
      }

      "must save the scoped declaration and redirect to Check Your Answers when the button is submitted (GET)" in {

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
              GET,
              routes.DeclarationController.onSubmitWithId(flowId).url
            )
              .withFormUrlEncodedBody()

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoadWithId(flowId).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          captor.getValue.get(DeclarationPage(id)) mustEqual Some(Set(Declaration.Confirmed))
          captor.getValue.get(DeclarationPage) mustEqual None
        }
      }

      "must save the declaration for the correct UUID without changing another journey" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()
        val firstId      = firstFlowId.toString
        val secondId     = secondFlowId.toString

        val userAnswers = scopedUserAnswers(firstFlowId)
          .set(CisIdPage(secondId), "second-cis-id")
          .success
          .value
          .set(DeclarationPage(secondId), Set(Declaration.Confirmed))
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
              GET,
              routes.DeclarationController.onSubmitWithId(firstFlowId).url
            )
              .withFormUrlEncodedBody()

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoadWithId(firstFlowId).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          val savedAnswers = captor.getValue

          savedAnswers.get(DeclarationPage(firstId)) mustEqual Some(Set(Declaration.Confirmed))
          savedAnswers.get(DeclarationPage(secondId)) mustEqual Some(Set(Declaration.Confirmed))
          savedAnswers.get(CisIdPage(secondId)) mustEqual Some("second-cis-id")
        }
      }

      "must redirect to Journey Recovery for a submission when no existing data is found" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = None)
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              routes.DeclarationController.onSubmitWithId(flowId).url
            )
              .withFormUrlEncodedBody()

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a submission when the scoped CisIdPage is missing" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              routes.DeclarationController.onSubmitWithId(flowId).url
            )
              .withFormUrlEncodedBody()

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the returns landing page for a submission when multi-tab is disabled" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> false)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              routes.DeclarationController.onSubmitWithId(flowId).url
            )
              .withFormUrlEncodedBody()

          val result = route(application, request).value

          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.returnsLandingPageUrl("test-cis-id", None)
        }
      }
    }
  }
}
