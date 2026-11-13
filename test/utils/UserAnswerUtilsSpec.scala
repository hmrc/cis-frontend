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

package utils

import base.SpecBase
import models.finalvalidation.MonthlyFinalValidationSource
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyNilReturn, MonthlyStandardReturn}
import models.monthlyreturns.{Declaration, SelectedSubcontractor, SubmissionConfirmationCache}
import models.submission.SubmissionDetails
import models.validation.SubcontractorValidationField.EmailAddress
import models.validation.{FieldValidationFailure, SubcontractorValidationFailure}
import models.{ReturnType, UserAnswers}
import pages.amend.WhichSubcontractorsToAddPage
import pages.finalvalidations.*
import pages.monthlyreturns.*
import pages.submission.*
import pages.validation.SubcontractorValidationFailuresPage
import utils.UserAnswerUtils.*

import java.time.{Instant, LocalDateTime}

class UserAnswerUtilsSpec extends SpecBase {

  private val validationFailures =
    List(
      SubcontractorValidationFailure(
        subcontractorId = 1L,
        failedFields = List(
          FieldValidationFailure(
            field = EmailAddress,
            value = Some("invalid-email")
          )
        )
      )
    )

  "UserAnswerUtils.isJourneyComplete" - {

    "when ReturnType is MonthlyNilReturn" - {

      "returns true when all nil return pages are answered (email not requested)" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyNilReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, false)
          .get
          .set(DeclarationPage, Set(Declaration.Confirmed))
          .get

        ua.isJourneyComplete mustBe true
      }

      "returns true when all nil return pages are answered (email requested and provided)" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyNilReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, true)
          .get
          .set(EnterYourEmailAddressPage, "test@test.com")
          .get
          .set(DeclarationPage, Set(Declaration.Confirmed))
          .get

        ua.isJourneyComplete mustBe true
      }

      "returns false when a required page is missing" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyNilReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          // SubmitInactivityRequestPage missing
          .set(ConfirmationByEmailPage, false)
          .get
          .set(DeclarationPage, Set(Declaration.Confirmed))
          .get

        ua.isJourneyComplete mustBe false
      }

      "returns false when email is requested but not provided" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyNilReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, true)
          .get
          // EnterYourEmailAddressPage missing
          .set(DeclarationPage, Set(Declaration.Confirmed))
          .get

        ua.isJourneyComplete mustBe false
      }

      "returns false when email is not requested but an email address is still saved (url-hopping scenario)" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyNilReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, false)
          .get
          .set(EnterYourEmailAddressPage, "stale@test.com")
          .get
          .set(DeclarationPage, Set(Declaration.Confirmed))
          .get

        ua.isJourneyComplete mustBe false
      }
    }

    "when ReturnType is MonthlyStandardReturn" - {

      "returns true when all standard return pages are answered (email not requested)" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyStandardReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SelectedSubcontractorPage(0), completeSub(0))
          .get
          .set(AllSubcontractorDetailsAdded, true)
          .get
          .set(PaymentDetailsConfirmationPage, true)
          .get
          .set(EmploymentStatusDeclarationPage, true)
          .get
          .set(VerifiedStatusDeclarationPage, true)
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, false)
          .get

        ua.isJourneyComplete mustBe true
      }

      "returns true when all standard return pages are answered (email requested and provided)" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyStandardReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SelectedSubcontractorPage(0), completeSub(0))
          .get
          .set(AllSubcontractorDetailsAdded, true)
          .get
          .set(PaymentDetailsConfirmationPage, true)
          .get
          .set(EmploymentStatusDeclarationPage, true)
          .get
          .set(VerifiedStatusDeclarationPage, true)
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, true)
          .get
          .set(EnterYourEmailAddressPage, "test@test.com")
          .get

        ua.isJourneyComplete mustBe true
      }

      "returns false when a required page is missing" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyStandardReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SelectedSubcontractorPage(0), completeSub(0))
          .get
          // SubcontractorDetailsAddedPage missing
          .set(PaymentDetailsConfirmationPage, true)
          .get
          .set(EmploymentStatusDeclarationPage, true)
          .get
          .set(VerifiedStatusDeclarationPage, true)
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, false)
          .get

        ua.isJourneyComplete mustBe false
      }

      "returns false when not all subcontractors are complete" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyStandardReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SelectedSubcontractorPage(0), incompleteSub(0))
          .get
          .set(AllSubcontractorDetailsAdded, true)
          .get
          .set(PaymentDetailsConfirmationPage, true)
          .get
          .set(EmploymentStatusDeclarationPage, true)
          .get
          .set(VerifiedStatusDeclarationPage, true)
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, false)
          .get

        ua.isJourneyComplete mustBe false
      }

      "returns false when SubcontractorDetailsAddedPage is false" in {
        val ua = UserAnswers("id")
          .set(ReturnTypePage, MonthlyStandardReturn)
          .get
          .set(DateConfirmPaymentsPage, java.time.LocalDate.now())
          .get
          .set(SelectedSubcontractorPage(0), completeSub(0))
          .get
          .set(AllSubcontractorDetailsAdded, false)
          .get
          .set(PaymentDetailsConfirmationPage, true)
          .get
          .set(EmploymentStatusDeclarationPage, true)
          .get
          .set(VerifiedStatusDeclarationPage, true)
          .get
          .set(SubmitInactivityRequestPage, true)
          .get
          .set(ConfirmationByEmailPage, false)
          .get

        ua.isJourneyComplete mustBe false
      }
    }

    "when ReturnType is not set" - {

      "returns false" in {
        UserAnswers("id").isJourneyComplete mustBe false
      }
    }
  }

  "UserAnswerUtils.firstIncompleteSubcontractorIndex" - {

    "returns 1 when there are no subcontractors" in {
      UserAnswers("id").firstIncompleteSubcontractorIndex mustBe 1
    }

    "returns 1 when all subcontractors are complete" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(0), completeSub(0))
        .setOrException(SelectedSubcontractorPage(1), completeSub(1))

      ua.firstIncompleteSubcontractorIndex mustBe 1
    }

    "returns the index of the single incomplete subcontractor" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(3), incompleteSub(3))

      ua.firstIncompleteSubcontractorIndex mustBe 3
    }

    "returns the minimum index among multiple incomplete subcontractors" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(0), completeSub(0))
        .setOrException(SelectedSubcontractorPage(2), incompleteSub(2))
        .setOrException(SelectedSubcontractorPage(5), incompleteSub(5))

      ua.firstIncompleteSubcontractorIndex mustBe 2
    }
  }

  "UserAnswerUtils.clearMonthlyReturnJourney" - {

    "removes all monthly return journey pages from UserAnswers" in {
      val submissionId = "submission-123"
      val ua           = UserAnswers("id")
        .set(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1))
        .get
        .set(SubmitInactivityRequestPage, true)
        .get
        .set(ConfirmationByEmailPage, true)
        .get
        .set(EnterYourEmailAddressPage, "test@example.com")
        .get
        .set(ResubmissionIdPage, 1L)
        .get
        .set(NilReturnStatusPage, "STARTED")
        .get
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .get
        .set(SelectedSubcontractorPage(1), completeSub(1))
        .get
        .set(VerifySubcontractorsPage, true)
        .get
        .set(AllSubcontractorDetailsAdded, true)
        .get
        .set(PaymentDetailsConfirmationPage, true)
        .get
        .set(EmploymentStatusDeclarationPage, true)
        .get
        .set(VerifiedStatusDeclarationPage, true)
        .get
        .set(SubcontractorValidationFailuresPage, validationFailures)
        .get
        .set(
          pages.amend.AmendmentDetailsPage,
          models.amend.AmendmentDetails("1", 2025, 1, "Test", MonthlyAmendedStandardReturn, None)
        )
        .get
        .set(WhichSubcontractorsToAddPage, Set("10"))
        .get
        .set(OriginalSubcontractorCountPage, 2)
        .get
        .set(FinalValidationDraftIdPage, "draft-123")
        .get
        .set(MonthlyFinalValidationContinuationPage, "NormalMode")
        .get
        .set(MonthlyFinalValidationSourcePage, MonthlyFinalValidationSource.SelectSubcontractors)
        .get
        .set(pages.monthlyreturns.IsResumedMonthlyReturnPage, true)
        .get
        .set(ConfirmEmailAddressPage, "test@example.com")
        .get
        .set(NilReturnStatusPage, "DRAFT")
        .get
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "ir-mark",
            submittedAt = LocalDateTime.of(2025, 1, 1, 12, 0)
          )
        )
        .get
        .set(
          SubmissionConfirmationCachePage,
          SubmissionConfirmationCache(
            periodEnd = "January 2025",
            contractorName = Some("Test Contractor Ltd"),
            email = "test@test.com",
            submittedTime = "12:00pm",
            submittedDate = "1 January 2025",
            submittedDateTimeIso = Some("2025-01-01T12:00:00Z")
          )
        )
        .get
        .set(PollUrlPage, "https://poll.example.com")
        .get
        .set(PollIntervalPage, 10)
        .get
        .set(CorrelationIdPage, "correlation-id")
        .get
        .set(LastMessageDatePage, Instant.parse("2025-01-01T12:00:00Z"))
        .get
        .set(SubmissionStatusTimedOutPage(submissionId), true)
        .get
        .set(SuccessEmailSentPage(submissionId), true)
        .get
        .set(SubmissionJourneyCompletedPage("2025-01"), true)
        .get
        .set(SubmissionCreatedPage("2025-01"), true)
        .flatMap(_.set(CreatedSubmissionIdPage("2025-01"), "sub-123"))
        .get

      val result = ua.clearMonthlyReturnJourney

      result.isSuccess mustBe true
      val cleared = result.get

      cleared.get(DateConfirmPaymentsPage) mustBe None
      cleared.get(SubmitInactivityRequestPage) mustBe None
      cleared.get(ConfirmationByEmailPage) mustBe None
      cleared.get(EnterYourEmailAddressPage) mustBe None
      cleared.get(ResubmissionIdPage) mustBe None
      cleared.get(NilReturnStatusPage) mustBe None
      cleared.get(DeclarationPage) mustBe None
      cleared.get(SelectedSubcontractorPage(1)) mustBe None
      cleared.get(VerifySubcontractorsPage) mustBe None
      cleared.get(AllSubcontractorDetailsAdded) mustBe None
      cleared.get(PaymentDetailsConfirmationPage) mustBe None
      cleared.get(EmploymentStatusDeclarationPage) mustBe None
      cleared.get(VerifiedStatusDeclarationPage) mustBe None
      cleared.get(SubcontractorValidationFailuresPage) mustBe None
      cleared.get(ConfirmEmailAddressPage) mustBe None
      cleared.get(NilReturnStatusPage) mustBe None
      cleared.get(pages.amend.AmendmentDetailsPage) mustBe None
      cleared.get(WhichSubcontractorsToAddPage) mustBe None
      cleared.get(OriginalSubcontractorCountPage) mustBe None
      cleared.get(FinalValidationDraftIdPage) mustBe None
      cleared.get(MonthlyFinalValidationContinuationPage) mustBe None
      cleared.get(MonthlyFinalValidationSourcePage) mustBe None
      cleared.get(pages.monthlyreturns.IsResumedMonthlyReturnPage) mustBe None

      cleared.get(SubmissionDetailsPage) mustBe None
      cleared.get(PollUrlPage) mustBe None
      cleared.get(PollIntervalPage) mustBe None
      cleared.get(CorrelationIdPage) mustBe None
      cleared.get(LastMessageDatePage) mustBe None
      cleared.get(SubmissionStatusTimedOutPage(submissionId)) mustBe None
      cleared.get(SuccessEmailSentPage(submissionId)) mustBe None
      cleared.get(SubmissionJourneyCompletedPage("2025-01")) mustBe None
      cleared.get(SubmissionCreatedPage("2025-01")) mustBe None
      cleared.get(CreatedSubmissionIdPage("2025-01")) mustBe None
      cleared.get(SubmissionConfirmationCachePage) mustBe None
    }

    "retains non-journey pages such as CisIdPage and ReturnTypePage" in {
      val ua = UserAnswers("id")
        .set(CisIdPage, "CIS-123")
        .get
        .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
        .get
        .set(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1))
        .get
        .set(VerifySubcontractorsPage, true)
        .get

      val result = ua.clearMonthlyReturnJourney

      result.isSuccess mustBe true
      val cleared = result.get

      cleared.get(CisIdPage) mustBe Some("CIS-123")
      cleared.get(ReturnTypePage) mustBe Some(ReturnType.MonthlyStandardReturn)
      cleared.get(DateConfirmPaymentsPage) mustBe None
      cleared.get(VerifySubcontractorsPage) mustBe None
    }

    "succeeds on empty UserAnswers" in {
      val result = UserAnswers("id").clearMonthlyReturnJourney
      result.isSuccess mustBe true
    }
  }

  "UserAnswerUtils.clearAmendedMonthlyStandardReturnJourney" - {

    "removes all monthly return journey pages from UserAnswers" in {
      val ua = UserAnswers("id")
        .set(EmploymentStatusDeclarationPage, true)
        .get
        .set(SelectedSubcontractorPage(1), completeSub(1))
        .get
        .set(VerifySubcontractorsPage, true)
        .get
        .set(AllSubcontractorDetailsAdded, true)
        .get
        .set(PaymentDetailsConfirmationPage, true)
        .get
        .set(VerifiedStatusDeclarationPage, true)
        .get
        .set(SubmitInactivityRequestPage, true)
        .get
        .set(WhichSubcontractorsToAddPage, Set("10"))
        .get
        .set(SubcontractorValidationFailuresPage, validationFailures)
        .get
        .set(FinalValidationDraftIdPage, "draft-456")
        .get
        .set(MonthlyFinalValidationContinuationPage, "CheckMode")
        .get
        .set(
          MonthlyFinalValidationSourcePage,
          MonthlyFinalValidationSource.WhichSubcontractorsToAdd("CheckMode")
        )
        .get
        .set(pages.monthlyreturns.IsResumedMonthlyReturnPage, true)
        .get
        .set(
          SubmissionConfirmationCachePage,
          SubmissionConfirmationCache(
            periodEnd = "January 2025",
            contractorName = Some("Test Contractor Ltd"),
            email = "test@test.com",
            submittedTime = "12:00pm",
            submittedDate = "1 January 2025",
            submittedDateTimeIso = Some("2025-01-01T12:00:00Z")
          )
        )
        .get

      val result = ua.clearAmendedMonthlyStandardReturnJourney

      result.isSuccess mustBe true
      val cleared = result.get

      cleared.get(EmploymentStatusDeclarationPage) mustBe None
      cleared.get(SelectedSubcontractorPage(1)) mustBe None
      cleared.get(VerifySubcontractorsPage) mustBe None
      cleared.get(AllSubcontractorDetailsAdded) mustBe None
      cleared.get(PaymentDetailsConfirmationPage) mustBe None
      cleared.get(VerifiedStatusDeclarationPage) mustBe None
      cleared.get(SubmitInactivityRequestPage) mustBe None
      cleared.get(WhichSubcontractorsToAddPage) mustBe None
      cleared.get(SubcontractorValidationFailuresPage) mustBe None
      cleared.get(SubmissionConfirmationCachePage) mustBe None
      cleared.get(FinalValidationDraftIdPage) mustBe None
      cleared.get(MonthlyFinalValidationContinuationPage) mustBe None
      cleared.get(MonthlyFinalValidationSourcePage) mustBe None
      cleared.get(pages.monthlyreturns.IsResumedMonthlyReturnPage) mustBe None
    }

    "retains non-journey pages such as CisIdPage and ReturnTypePage" in {
      val ua = UserAnswers("id")
        .set(CisIdPage, "CIS-123")
        .get
        .set(ReturnTypePage, ReturnType.MonthlyAmendedNilReturn)
        .get
        .set(EmploymentStatusDeclarationPage, true)
        .get
        .set(VerifySubcontractorsPage, true)
        .get

      val result = ua.clearAmendedMonthlyStandardReturnJourney

      result.isSuccess mustBe true
      val cleared = result.get

      cleared.get(CisIdPage) mustBe Some("CIS-123")
      cleared.get(ReturnTypePage) mustBe Some(ReturnType.MonthlyAmendedNilReturn)
      cleared.get(EmploymentStatusDeclarationPage) mustBe None
      cleared.get(VerifySubcontractorsPage) mustBe None
    }

    "succeeds on empty UserAnswers" in {
      val result = UserAnswers("id").clearAmendedMonthlyStandardReturnJourney
      result.isSuccess mustBe true
    }
  }

  "returns true when all ID-scoped nil return pages are answered (email not requested)" in {
    val id = "test-id"

    val ua = UserAnswers("id")
      .setOrException(ReturnTypePage(id), MonthlyNilReturn)
      .setOrException(DateConfirmPaymentsPage(id), java.time.LocalDate.of(2025, 1, 1))
      .setOrException(SubmitInactivityRequestPage(id), true)
      .setOrException(ConfirmationByEmailPage(id), false)
      .setOrException(DeclarationPage(id), Set(Declaration.Confirmed))

    ua.isJourneyComplete(id) mustBe true
    ua.setOrException(ReturnTypePage(id), MonthlyAmendedNilReturn).isJourneyComplete(id) mustBe true
  }

  "returns true when email is requested and provided" in {
    val id = "test-id"

    val ua = UserAnswers("id")
      .setOrException(ReturnTypePage(id), MonthlyNilReturn)
      .setOrException(DateConfirmPaymentsPage(id), java.time.LocalDate.of(2025, 1, 1))
      .setOrException(SubmitInactivityRequestPage(id), true)
      .setOrException(ConfirmationByEmailPage(id), true)
      .setOrException(DeclarationPage(id), Set(Declaration.Confirmed))

    ua.isJourneyComplete(id) mustBe false

    val withEmail = ua.setOrException(EnterYourEmailAddressPage(id), "test@test.com")

    withEmail.isJourneyComplete(id) mustBe true

    val withoutEmail = withEmail.setOrException(ConfirmationByEmailPage(id), false)

    withoutEmail.get(EnterYourEmailAddressPage(id)) mustBe None
    withoutEmail.isJourneyComplete(id) mustBe true
  }

  "returns false when required ID-scoped answers are missing" in {
    val id = "test-id"

    val ua = UserAnswers("id")
      .setOrException(ReturnTypePage(id), MonthlyNilReturn)
      .setOrException(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1))
      .setOrException(SubmitInactivityRequestPage(id), true)
      .setOrException(ConfirmationByEmailPage(id), false)

    ua.isJourneyComplete(id) mustBe false
    UserAnswers("id").isJourneyComplete(id) mustBe false
  }

  "uses answers from the correct ID-scoped journey" in {
    val ua = UserAnswers("id")
      .setOrException(ReturnTypePage("first-id"), MonthlyNilReturn)
      .setOrException(DateConfirmPaymentsPage("first-id"), java.time.LocalDate.of(2025, 1, 1))
      .setOrException(SubmitInactivityRequestPage("first-id"), true)
      .setOrException(ConfirmationByEmailPage("first-id"), false)
      .setOrException(DeclarationPage("first-id"), Set(Declaration.Confirmed))
      .setOrException(ReturnTypePage("second-id"), MonthlyNilReturn)
      .setOrException(ReturnTypePage, MonthlyNilReturn)
      .setOrException(DeclarationPage, Set(Declaration.Confirmed))

    ua.isJourneyComplete("first-id") mustBe true
    ua.isJourneyComplete("second-id") mustBe false
  }

  "UserAnswerUtils.clearMonthlyReturnJourney with id" - {

    "removes ID-scoped nil return and submission pages" in {
      val id     = "test-id"
      val period = "2025-01"

      val ua = UserAnswers("id")
        .setOrException(CisIdPage(id), "CIS-123")
        .setOrException(ReturnTypePage(id), MonthlyNilReturn)
        .setOrException(DateConfirmPaymentsPage(id), java.time.LocalDate.of(2025, 1, 1))
        .setOrException(SubmitInactivityRequestPage(id), true)
        .setOrException(ConfirmationByEmailPage(id), false)
        .setOrException(DeclarationPage(id), Set(Declaration.Confirmed))
        .setOrException(ResubmissionIdPage(id), 123L)
        .setOrException(IsResumedMonthlyReturnPage(id), true)
        .setOrException(PollUrlPage(id), "https://poll.example.com")
        .setOrException(PollIntervalPage(id), 10)
        .setOrException(CorrelationIdPage(id), "correlation-id")
        .setOrException(SubmissionCreatedForFlowPage(id, period), true)
        .setOrException(CreatedSubmissionIdForFlowPage(id, period), "sub-123")
        .setOrException(SubmissionJourneyCompletedForFlowPage(id, period), true)

      val cleared = ua.clearMonthlyReturnJourney(id).get

      cleared.get(CisIdPage(id)) mustBe None
      cleared.get(ReturnTypePage(id)) mustBe None
      cleared.get(DateConfirmPaymentsPage(id)) mustBe None
      cleared.get(SubmitInactivityRequestPage(id)) mustBe None
      cleared.get(ConfirmationByEmailPage(id)) mustBe None
      cleared.get(DeclarationPage(id)) mustBe None
      cleared.get(ResubmissionIdPage(id)) mustBe None
      cleared.get(IsResumedMonthlyReturnPage(id)) mustBe None
      cleared.get(PollUrlPage(id)) mustBe None
      cleared.get(PollIntervalPage(id)) mustBe None
      cleared.get(CorrelationIdPage(id)) mustBe None
      cleared.get(SubmissionCreatedForFlowPage(id, period)) mustBe None
      cleared.get(CreatedSubmissionIdForFlowPage(id, period)) mustBe None
      cleared.get(SubmissionJourneyCompletedForFlowPage(id, period)) mustBe None
    }

    "retains legacy and other ID-scoped journey answers" in {
      val ua = UserAnswers("id")
        .setOrException(CisIdPage, "CIS-legacy")
        .setOrException(ReturnTypePage, MonthlyNilReturn)
        .setOrException(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1))
        .setOrException(CisIdPage("first-id"), "CIS-123")
        .setOrException(ReturnTypePage("first-id"), MonthlyNilReturn)
        .setOrException(DateConfirmPaymentsPage("first-id"), java.time.LocalDate.of(2025, 1, 1))
        .setOrException(CisIdPage("second-id"), "CIS-456")
        .setOrException(ReturnTypePage("second-id"), MonthlyNilReturn)
        .setOrException(DateConfirmPaymentsPage("second-id"), java.time.LocalDate.of(2025, 2, 1))
        .setOrException(SubmissionCreatedPage("2025-01"), true)
        .setOrException(SubmissionCreatedForFlowPage("first-id", "2025-01"), true)
        .setOrException(SubmissionCreatedForFlowPage("second-id", "2025-02"), true)

      val cleared = ua.clearMonthlyReturnJourney("first-id").get

      cleared.get(CisIdPage("first-id")) mustBe None
      cleared.get(ReturnTypePage("first-id")) mustBe None
      cleared.get(DateConfirmPaymentsPage("first-id")) mustBe None
      cleared.get(SubmissionCreatedForFlowPage("first-id", "2025-01")) mustBe None

      cleared.get(CisIdPage("second-id")) mustBe Some("CIS-456")
      cleared.get(ReturnTypePage("second-id")) mustBe Some(MonthlyNilReturn)
      cleared.get(DateConfirmPaymentsPage("second-id")) mustBe Some(java.time.LocalDate.of(2025, 2, 1))
      cleared.get(SubmissionCreatedForFlowPage("second-id", "2025-02")) mustBe Some(true)

      cleared.get(CisIdPage) mustBe Some("CIS-legacy")
      cleared.get(ReturnTypePage) mustBe Some(MonthlyNilReturn)
      cleared.get(DateConfirmPaymentsPage) mustBe Some(java.time.LocalDate.of(2025, 1, 1))
      cleared.get(SubmissionCreatedPage("2025-01")) mustBe Some(true)
    }

    "succeeds on empty UserAnswers" in {
      val result = UserAnswers("id").clearMonthlyReturnJourney("test-id")

      result.isSuccess mustBe true
    }
  }

  private def completeSub(index: Int) = SelectedSubcontractor(
    id = index.toLong,
    name = s"Sub $index",
    totalPaymentsMade = Some(100),
    costOfMaterials = Some(50),
    totalTaxDeducted = Some(20)
  )

  private def incompleteSub(index: Int) = SelectedSubcontractor(
    id = index.toLong,
    name = s"Sub $index",
    totalPaymentsMade = None,
    costOfMaterials = None,
    totalTaxDeducted = None
  )
}
