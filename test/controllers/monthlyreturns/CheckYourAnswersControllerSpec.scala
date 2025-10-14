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
import models.{ChrisResult, UserAnswers}
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.govuk.SummaryListFluency
import viewmodels.checkAnswers.monthlyreturns.{PaymentsToSubcontractorsSummary, ReturnTypeSummary}
import views.html.monthlyreturns.CheckYourAnswersView
import services.MonthlyReturnService
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, DeclarationPage, InactivityRequestPage}
import models.monthlyreturns.{Declaration, InactivityRequest}
import java.time.LocalDate
import scala.concurrent.Future
import com.google.inject.AbstractModule

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view              = application.injector.instanceOf[CheckYourAnswersView]
        val returnDetailsList = SummaryListViewModel(
          Seq(
            ReturnTypeSummary.row(messages(application)).get,
            PaymentsToSubcontractorsSummary.row(messages(application)).get
          )
        )
        val emailList         = SummaryListViewModel(Seq.empty)

        status(result) mustEqual OK
        val rendered = view(returnDetailsList, emailList)(request, messages(application)).toString
        contentAsString(result) mustEqual rendered
      }
    }

    "must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to stub when stub enabled and service not hit" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.stub-sending-enabled" -> true)
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual stub.controllers.monthlyreturns.routes.StubSubmissionSendingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to submission sending on POST" in {
      val mockService: MonthlyReturnService = mock(classOf[MonthlyReturnService])
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]()))
        .thenReturn(scala.concurrent.Future.successful(ChrisResult.Submitted))
    "must redirect to submission sending on POST when monthly return creation succeeds" in {

      val userAnswers = emptyUserAnswers
        .set(CisIdPage, "test-cis-id")
        .success
        .value
        .set(DateConfirmNilPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .success
        .value
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[MonthlyReturnService].toInstance(mockService))
          .build()
      val mockService = mock[MonthlyReturnService]
      when(mockService.createNilMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(userAnswers))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          new AbstractModule {
            override def configure(): Unit =
              bind(classOf[MonthlyReturnService]).toInstance(mockService)
          }
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubmissionSendingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to Journey Recovery when service returns Rejected" in {
      val mockService: MonthlyReturnService = mock(classOf[MonthlyReturnService])
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]()))
        .thenReturn(scala.concurrent.Future.successful(ChrisResult.Rejected(NOT_MODIFIED, "Not modified")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[MonthlyReturnService].toInstance(mockService))
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when service returns UpstreamFailed" in {
      val mockService: MonthlyReturnService = mock(classOf[MonthlyReturnService])
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]()))
        .thenReturn(scala.concurrent.Future.successful(ChrisResult.UpstreamFailed(INTERNAL_SERVER_ERROR, "boom")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[MonthlyReturnService].toInstance(mockService))
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on POST when service throws " in {
      val mockService: MonthlyReturnService = mock(classOf[MonthlyReturnService])
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[MonthlyReturnService].toInstance(mockService))
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]())
      }
    }

    "must redirect to Journey Recovery for POST if no existing data is found" in {
      val mockService: MonthlyReturnService = mock(classOf[MonthlyReturnService])

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[MonthlyReturnService].toInstance(mockService))
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }


    "must redirect to journey recovery on POST when monthly return creation fails" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
