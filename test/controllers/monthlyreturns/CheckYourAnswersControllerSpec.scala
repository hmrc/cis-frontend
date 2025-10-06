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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.govuk.SummaryListFluency
import viewmodels.checkAnswers.monthlyreturns.{PaymentsToSubcontractorsSummary, ReturnTypeSummary}
import views.html.monthlyreturns.CheckYourAnswersView
import services.MonthlyReturnService
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, DeclarationPage, InactivityRequestPage}
import models.monthlyreturns.{Declaration, InactivityRequest}

import java.time.LocalDate
import scala.concurrent.Future
import com.google.inject.AbstractModule
import models.requests.DataRequest
import services.guard.{DuplicateCreationCheck, DuplicateCreationGuard}
import services.guard.DuplicateCreationCheck.{DuplicateFound, NoDuplicate}

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private val allowGuard = new DuplicateCreationGuard {
    def check(implicit request: DataRequest[_]) = Future.successful(NoDuplicate)
  }
  private val blockGuard = new DuplicateCreationGuard {
    def check(implicit r: DataRequest[_]) = Future.successful(DuplicateFound)
  }


  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

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

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

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

      val mockService = mock[MonthlyReturnService]
      when(mockService.createNilMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(userAnswers))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          new AbstractModule {
            override def configure(): Unit =
              bind(classOf[MonthlyReturnService]).toInstance(mockService)
              bind(classOf[DuplicateCreationGuard]).toInstance(allowGuard)
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

    "must redirect to journey recovery on POST when monthly return creation fails" in {
      val mockService = mock[MonthlyReturnService]
      when(mockService.createNilMonthlyReturn(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          new AbstractModule {
            override def configure(): Unit = {
              bind(classOf[MonthlyReturnService]).toInstance(mockService)
              bind(classOf[DuplicateCreationGuard]).toInstance(allowGuard)
            }
          }
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockService, times(1)).createNilMonthlyReturn(any())(any())
      }
    }

    "must redirect to Journey Recovery on POST when duplicate is found (guard blocks) and not call service" in {
      val mockService = mock[MonthlyReturnService]
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          new AbstractModule {
            override def configure(): Unit = {
              bind(classOf[MonthlyReturnService]).toInstance(mockService)
              bind(classOf[DuplicateCreationGuard]).toInstance(blockGuard)
            }
          }
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verifyNoInteractions(mockService)
      }
    }
  }
}
