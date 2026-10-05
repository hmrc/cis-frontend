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
import models.monthlyreturns.Declaration.Confirmed
import models.requests.CisPath.CisOrg
import models.{ReturnType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import pages.submission.SubmissionJourneyCompletedPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import services.submission.SubmissionService
import viewmodels.checkAnswers.monthlyreturns.*
import viewmodels.govuk.SummaryListFluency
import views.html.monthlyreturns.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private val completeAnswers = emptyUserAnswers
    .setOrException(ReturnTypePage, ReturnType.MonthlyNilReturn)
    .setOrException(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
    .setOrException(SubmitInactivityRequestPage, true)
    .setOrException(ConfirmationByEmailPage, false)
    .setOrException(DeclarationPage, Set(Confirmed))

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value
        .set(SubmitInactivityRequestPage, true)
        .success
        .value
        .set(ConfirmationByEmailPage, false)
        .success
        .value
        .set(DeclarationPage, Set(Confirmed))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(CisOrg).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val returnDetailsList = SummaryListViewModel(
          rows = Seq(
            ReturnTypeSummary.row(userAnswers)(messages(application)),
            DateConfirmNilPaymentsSummary.row(userAnswers)(messages(application)),
            PaymentsToSubcontractorsSummary.row(messages(application)),
            SubmitInactivityRequestSummary.row(CisOrg, userAnswers)(messages(application))
          ).flatten
        )

        val emailList = SummaryListViewModel(
          rows = Seq(
            ConfirmationByEmailSummary.row(CisOrg, userAnswers)(messages(application)),
            EnterYourEmailAddressSummary.row(CisOrg, userAnswers)(messages(application))
          ).flatten
        )

        status(result) mustEqual OK
        val rendered = view(CisOrg, returnDetailsList, emailList)(request, messages(application)).toString
        contentAsString(result) mustEqual rendered
      }
    }

    "must include Return period ended and ConfirmationByEmail rows for monthly standard return" in {

      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
        .success
        .value
        .set(EmploymentStatusDeclarationPage, true)
        .success
        .value
        .set(VerifiedStatusDeclarationPage, true)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 2, 5))
        .success
        .value
        .set(SubmitInactivityRequestPage, true)
        .success
        .value
        .set(ConfirmationByEmailPage, true)
        .success
        .value
        .set(DeclarationPage, Set(Confirmed))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(CisOrg).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val returnDetailsList = SummaryListViewModel(
          rows = Seq(
            ReturnTypeSummary.row(userAnswers)(messages(application)),
            DateConfirmPaymentsSummary.row(userAnswers)(messages(application)),
            EmploymentStatusDeclarationSummary.row(userAnswers)(messages(application)),
            VerifiedStatusDeclarationSummary.row(userAnswers)(messages(application)),
            SubmitInactivityRequestSummary.row(CisOrg, userAnswers)(messages(application))
          ).flatten
        )

        val emailList = SummaryListViewModel(
          rows = Seq(
            ConfirmationByEmailSummary.row(CisOrg, userAnswers)(messages(application)),
            EnterYourEmailAddressSummary.row(CisOrg, userAnswers)(messages(application))
          ).flatten
        )

        status(result) mustEqual OK
        val rendered = view(CisOrg, returnDetailsList, emailList)(request, messages(application)).toString
        contentAsString(result) mustEqual rendered
      }
    }

    "must include Email address row in emailList when confirmation by email is Yes and email is entered" in {

      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
        .success
        .value
        .set(EmploymentStatusDeclarationPage, true)
        .success
        .value
        .set(VerifiedStatusDeclarationPage, true)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 2, 5))
        .success
        .value
        .set(SubmitInactivityRequestPage, true)
        .success
        .value
        .set(ConfirmationByEmailPage, true)
        .success
        .value
        .set(EnterYourEmailAddressPage, "test@example.com")
        .success
        .value
        .set(DeclarationPage, Set(Confirmed))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(CisOrg).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val returnDetailsList = SummaryListViewModel(
          rows = Seq(
            ReturnTypeSummary.row(userAnswers)(messages(application)),
            DateConfirmPaymentsSummary.row(userAnswers)(messages(application)),
            EmploymentStatusDeclarationSummary.row(userAnswers)(messages(application)),
            VerifiedStatusDeclarationSummary.row(userAnswers)(messages(application)),
            SubmitInactivityRequestSummary.row(CisOrg, userAnswers)(messages(application))
          ).flatten
        )

        val emailList = SummaryListViewModel(
          rows = Seq(
            ConfirmationByEmailSummary.row(CisOrg, userAnswers)(messages(application)),
            EnterYourEmailAddressSummary.row(CisOrg, userAnswers)(messages(application))
          ).flatten
        )

        status(result) mustEqual OK
        val rendered = view(CisOrg, returnDetailsList, emailList)(request, messages(application)).toString
        contentAsString(result) mustEqual rendered
      }
    }

    "must redirect to journey recovery on GET when DateConfirmPaymentsPage is missing" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery on POST when Journey is incomplete" in {
      val userAnswers = completeAnswers.remove(SubmitInactivityRequestPage).get

      val mockService = mock[MonthlyReturnService]
      when(mockService.updateMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must call updateMonthlyReturn and redirect to submission sending on POST when Journey is complete" in {
      val userAnswers = completeAnswers
      val mockService = mock[MonthlyReturnService]
      when(mockService.updateMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val mockSubmissionService = mock[SubmissionService]

      when(mockSubmissionService.isAlreadySubmitted(any[UserAnswers]))
        .thenReturn(false)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SubmissionService].toInstance(mockSubmissionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SubmissionSendingController.onPageLoad(CisOrg).url
      }
    }

    "must redirect to journey recovery on POST when ReturnTypePage is missing" in {

      val userAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery on POST when journey is incomplete is missing" in {

      val userAnswers = completeAnswers.remove(SubmitInactivityRequestPage).get
      val mockService = mock[MonthlyReturnService]
      when(mockService.updateMonthlyReturn(any())(any()))
        .thenReturn(Future.successful(()))

      val mockSubmissionService = mock[SubmissionService]

      when(mockSubmissionService.isAlreadySubmitted(any[UserAnswers]))
        .thenReturn(false)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SubmissionService].toInstance(mockSubmissionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to SystemError on POST when updateNilMonthlyReturn fails" in {

      val userAnswers = completeAnswers

      val mockService           = mock[MonthlyReturnService]
      when(mockService.updateMonthlyReturn(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("service error")))
      val mockSubmissionService = mock[SubmissionService]

      when(mockSubmissionService.isAlreadySubmitted(any[UserAnswers]))
        .thenReturn(false)
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SubmissionService].toInstance(mockSubmissionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to journey recovery on POST when submission is already created" in {
      val userAnswers = completeAnswers

      val mockService           = mock[MonthlyReturnService]
      val mockSubmissionService = mock[SubmissionService]

      when(mockSubmissionService.isAlreadySubmitted(any[UserAnswers]))
        .thenReturn(true)
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SubmissionService].toInstance(mockSubmissionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AlreadySubmittedController.onPageLoad().url
      }
    }

    "must redirect to already submitted page for a GET when submission journey is already completed" in {
      val userAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionJourneyCompletedPage("2025-08"), true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AlreadySubmittedController.onPageLoad().url
      }
    }

    "must redirect to already submitted page on POST when submission journey is already completed" in {
      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionJourneyCompletedPage("2025-08"), true)
        .success
        .value

      val mockService           = mock[MonthlyReturnService]
      val mockSubmissionService = mock[SubmissionService]

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SubmissionService].toInstance(mockSubmissionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AlreadySubmittedController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery on POST when update request cannot be built from UserAnswers" in {

      val userAnswers = spy(completeAnswers)

      doReturn(
        Some(LocalDate.of(2024, 3, 1)),
        None
      ).when(userAnswers).get(DateConfirmPaymentsPage)

      val mockService           = mock[MonthlyReturnService]
      val mockSubmissionService = mock[SubmissionService]

      when(mockSubmissionService.isAlreadySubmitted(any[UserAnswers]))
        .thenReturn(false)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SubmissionService].toInstance(mockSubmissionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(CisOrg).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
