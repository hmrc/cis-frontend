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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import viewmodels.govuk.SummaryListFluency
import viewmodels.checkAnswers.monthlyreturns.{PaymentsToSubcontractorsSummary, ReturnTypeSummary}
import views.html.nilreturns.CheckYourAnswersView
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, NilReturnStatusPage}
import java.time.LocalDate

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

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
        ).value mustEqual controllers.routes.UnauthorisedOrganisationAffinityController
          .onPageLoad()
          .url
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

    "must call updateNilMonthlyReturn and redirect to submission sending on POST when FormP record already exists (NilReturnStatusPage set)" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, "test-cis-id")
        .success
        .value
        .set(DateConfirmNilPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value
        .set(NilReturnStatusPage, "STARTED")
        .success
        .value

      val mockService = mock[MonthlyReturnService]
      when(mockService.updateNilMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
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

    "must redirect to journey recovery on POST when NilReturnStatusPage is missing" in {

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
