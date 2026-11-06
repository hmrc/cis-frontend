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
import models.monthlyreturns.Declaration.Confirmed
import models.{CheckMode, ReturnType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import services.submission.SubmissionService
import viewmodels.checkAnswers.monthlyreturns.*
import viewmodels.govuk.SummaryListFluency
import views.html.monthlyreturns.CheckYourAnswersView
import pages.submission.SubmissionJourneyCompletedPage

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private val completeAnswers = emptyUserAnswers
    .setOrException(ReturnTypePage, ReturnType.MonthlyNilReturn)
    .setOrException(CisIdPage, "test-cis-id")
    .setOrException(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
    .setOrException(SubmitInactivityRequestPage, true)
    .setOrException(ConfirmationByEmailPage, false)
    .setOrException(DeclarationPage, Set(Confirmed))

  private def completeScopedAnswers(flowId: UUID, returnType: ReturnType = ReturnType.MonthlyNilReturn) = {
    val id = flowId.toString

    emptyUserAnswers
      .setOrException(CisIdPage(id), "test-cis-id")
      .setOrException(ReturnTypePage(id), returnType)
      .setOrException(DateConfirmPaymentsPage(id), LocalDate.of(2024, 3, 1))
      .setOrException(SubmitInactivityRequestPage(id), true)
      .setOrException(ConfirmationByEmailPage(id), false)
      .setOrException(DeclarationPage(id), Set(Confirmed))
  }

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
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val returnDetailsList = SummaryListViewModel(
          rows = Seq(
            ReturnTypeSummary.row(userAnswers)(messages(application)),
            DateConfirmNilPaymentsSummary.row(userAnswers)(messages(application)),
            PaymentsToSubcontractorsSummary.row(messages(application)),
            SubmitInactivityRequestSummary.row(userAnswers)(messages(application))
          ).flatten
        )

        val emailList = SummaryListViewModel(
          rows = Seq(
            ConfirmationByEmailSummary.row(userAnswers)(messages(application)),
            EnterYourEmailAddressSummary.row(userAnswers)(messages(application))
          ).flatten
        )

        status(result) mustEqual OK
        val rendered = view(returnDetailsList, emailList)(request, messages(application)).toString
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
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val returnDetailsList = SummaryListViewModel(
          rows = Seq(
            ReturnTypeSummary.row(userAnswers)(messages(application)),
            DateConfirmPaymentsSummary.row(userAnswers)(messages(application)),
            EmploymentStatusDeclarationSummary.row(userAnswers)(messages(application)),
            VerifiedStatusDeclarationSummary.row(userAnswers)(messages(application)),
            SubmitInactivityRequestSummary.row(userAnswers)(messages(application))
          ).flatten
        )

        val emailList = SummaryListViewModel(
          rows = Seq(
            ConfirmationByEmailSummary.row(userAnswers)(messages(application)),
            EnterYourEmailAddressSummary.row(userAnswers)(messages(application))
          ).flatten
        )

        status(result) mustEqual OK
        val rendered = view(returnDetailsList, emailList)(request, messages(application)).toString
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
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val returnDetailsList = SummaryListViewModel(
          rows = Seq(
            ReturnTypeSummary.row(userAnswers)(messages(application)),
            DateConfirmPaymentsSummary.row(userAnswers)(messages(application)),
            EmploymentStatusDeclarationSummary.row(userAnswers)(messages(application)),
            VerifiedStatusDeclarationSummary.row(userAnswers)(messages(application)),
            SubmitInactivityRequestSummary.row(userAnswers)(messages(application))
          ).flatten
        )

        val emailList = SummaryListViewModel(
          rows = Seq(
            ConfirmationByEmailSummary.row(userAnswers)(messages(application)),
            EnterYourEmailAddressSummary.row(userAnswers)(messages(application))
          ).flatten
        )

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

    "must redirect to journey recovery on GET when DateConfirmPaymentsPage is missing" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubmissionSendingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to journey recovery on POST when ReturnTypePage is missing" in {

      val userAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.AlreadySubmittedController
          .onPageLoad()
          .url
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
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url
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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url
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
        val request = FakeRequest(POST, controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "UUID-scoped journey" - {

      "must return OK and the correct view for a nil return GET" in {

        val flowId      = UUID.randomUUID()
        val id          = flowId.toString
        val userAnswers = completeScopedAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val returnDetailsList = SummaryListViewModel(
            rows = Seq(
              ReturnTypeSummary.row(userAnswers, id)(messages(application)),
              DateConfirmNilPaymentsSummary.row(userAnswers, id)(messages(application)),
              PaymentsToSubcontractorsSummary.row(messages(application)),
              SubmitInactivityRequestSummary.row(
                userAnswers,
                id,
                controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, CheckMode)
              )(messages(application))
            ).flatten
          )

          val emailList = SummaryListViewModel(
            rows = Seq(
              ConfirmationByEmailSummary.row(
                userAnswers,
                id,
                controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(flowId, CheckMode)
              )(messages(application)),
              EnterYourEmailAddressSummary.row(
                userAnswers,
                id,
                controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, CheckMode)
              )(messages(application))
            ).flatten
          )

          val formAction = Some(controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmitWithId(flowId))

          status(result) mustEqual OK
          val rendered = view(returnDetailsList, emailList, formAction)(request, messages(application)).toString
          contentAsString(result) mustEqual rendered
        }
      }

      "must return OK for an amended nil return GET" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = completeScopedAnswers(flowId, ReturnType.MonthlyAmendedNilReturn)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(
            controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmitWithId(flowId).url
          )
        }
      }

      "must include Email address row when confirmation by email is Yes and email is entered" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = completeScopedAnswers(flowId)
          .setOrException(ConfirmationByEmailPage(id), true)
          .setOrException(EnterYourEmailAddressPage(id), "test@example.com")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("test@example.com")
          contentAsString(result) must include(
            controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, CheckMode).url
          )
        }
      }

      "must display answers from the correct UUID-scoped journey" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()
        val firstId      = firstFlowId.toString
        val secondId     = secondFlowId.toString

        val userAnswers = completeScopedAnswers(firstFlowId)
          .setOrException(ConfirmationByEmailPage(firstId), true)
          .setOrException(EnterYourEmailAddressPage(firstId), "first@example.com")
          .setOrException(CisIdPage(secondId), "second-cis-id")
          .setOrException(ReturnTypePage(secondId), ReturnType.MonthlyNilReturn)
          .setOrException(DateConfirmPaymentsPage(secondId), LocalDate.of(2025, 4, 1))
          .setOrException(SubmitInactivityRequestPage(secondId), false)
          .setOrException(ConfirmationByEmailPage(secondId), true)
          .setOrException(EnterYourEmailAddressPage(secondId), "second@example.com")
          .setOrException(DeclarationPage(secondId), Set(Confirmed))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val firstRequest = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(firstFlowId).url
          )

          val secondRequest = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(secondFlowId).url
          )

          val firstResult  = route(application, firstRequest).value
          val secondResult = route(application, secondRequest).value

          status(firstResult) mustEqual OK
          status(secondResult) mustEqual OK

          contentAsString(firstResult) must include("first@example.com")
          contentAsString(firstResult).contains("second@example.com") mustEqual false

          contentAsString(secondResult) must include("second@example.com")
          contentAsString(secondResult).contains("first@example.com") mustEqual false
        }
      }

      "must redirect to Journey Recovery when the flowId is not found" in {

        val existingFlowId = UUID.randomUUID()
        val missingFlowId  = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = Some(completeScopedAnswers(existingFlowId)))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(missingFlowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must not use legacy answers when the UUID-scoped journey is missing" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = completeAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped ReturnTypePage is missing" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = emptyUserAnswers
          .setOrException(CisIdPage(id), "test-cis-id")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped return type is not a nil return" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = completeScopedAnswers(flowId, ReturnType.MonthlyStandardReturn)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the returns landing page when multi-tab is disabled" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = completeScopedAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )

          val result = route(application, request).value

          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.returnsLandingPageUrl("test-cis-id", None)
        }
      }

      "must return Not Implemented for a POST until ChRIS submission is implemented" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = completeScopedAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmitWithId(flowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual NOT_IMPLEMENTED
        }
      }

      "must redirect to Journey Recovery for a POST when the flowId is not found" in {

        val existingFlowId = UUID.randomUUID()
        val missingFlowId  = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = Some(completeScopedAnswers(existingFlowId)))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.monthlyreturns.routes.CheckYourAnswersController.onSubmitWithId(missingFlowId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
