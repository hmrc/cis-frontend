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

import models.ReturnType.*
import models.UserAnswers
import pages.QuestionPage
import pages.monthlyreturns.*
import pages.amend.*
import pages.agent.AgentClientDataPage
import pages.finalvalidations.*
import pages.submission.*
import pages.validation.SubcontractorValidationFailuresPage
import play.api.libs.json.Reads

import scala.util.Try
import java.time.YearMonth

object UserAnswerUtils {

  extension (userAnswers: UserAnswers) {

    private def answered[A](page: QuestionPage[A])(using Reads[A]): Boolean =
      userAnswers.get(page).nonEmpty

    private def isTrue(page: QuestionPage[Boolean]): Boolean =
      userAnswers.get(page).contains(true)

    private def allSubcontractorsComplete: Boolean =
      userAnswers
        .get(SelectedSubcontractorPage.all)
        .getOrElse(Map.empty)
        .values
        .forall(_.isComplete)

    private def emailSatisfied: Boolean =
      userAnswers.get(ConfirmationByEmailPage) match {
        case Some(true)  => answered(EnterYourEmailAddressPage)
        case Some(false) => !answered(EnterYourEmailAddressPage)
        case None        => false
      }

    // Multi-tab: Check whether the email answer is satisfied for the ID-scoped journey.
    private def emailSatisfied(id: String): Boolean =
      userAnswers.get(ConfirmationByEmailPage(id)) match {
        case Some(true)  => answered(EnterYourEmailAddressPage(id))
        case Some(false) => !answered(EnterYourEmailAddressPage(id))
        case None        => false
      }

    def firstIncompleteSubcontractorIndex: Int =
      userAnswers
        .get(SelectedSubcontractorPage.all)
        .getOrElse(Map.empty)
        .collect { case (idx, subcontractor) if !subcontractor.isComplete => idx }
        .minOption
        .getOrElse(1)

    def incompleteSubcontractorIds: Seq[Long] =
      userAnswers
        .get(SelectedSubcontractorPage.all)
        .getOrElse(Map.empty)
        .values
        .iterator
        .filterNot(_.isComplete)
        .map(_.id)
        .toSeq

    def clearMonthlyReturnJourney: Try[UserAnswers] = {
      val submissionId = userAnswers.get(SubmissionDetailsPage).map(_.id)
      val period       = userAnswers.get(DateConfirmPaymentsPage).map(d => YearMonth.from(d).toString)

      val clearedAnswers = userAnswers
        // common
        .remove(DateConfirmPaymentsPage)
        .flatMap(_.remove(SubmitInactivityRequestPage))
        .flatMap(_.remove(ConfirmationByEmailPage))
        .flatMap(_.remove(EnterYourEmailAddressPage))
        .flatMap(_.remove(ResubmissionIdPage))

        // monthly nil return
        .flatMap(_.remove(NilReturnStatusPage))
        .flatMap(_.remove(ConfirmEmailAddressPage))
        .flatMap(_.remove(DeclarationPage))

        // monthly standard return
        .flatMap(_.remove(SelectedSubcontractorPage.all))
        .flatMap(_.remove(VerifySubcontractorsPage))
        .flatMap(_.remove(AllSubcontractorDetailsAdded))
        .flatMap(_.remove(PaymentDetailsConfirmationPage))
        .flatMap(_.remove(EmploymentStatusDeclarationPage))
        .flatMap(_.remove(VerifiedStatusDeclarationPage))
        .flatMap(_.remove(SubcontractorValidationFailuresPage))

        // amendment-specific state
        .flatMap(_.remove(AmendmentDetailsPage))
        .flatMap(_.remove(WhichSubcontractorsToAddPage))
        .flatMap(_.remove(OriginalSubcontractorCountPage))

        // final validation state
        .flatMap(_.remove(FinalValidationDraftIdPage))
        .flatMap(_.remove(MonthlyFinalValidationContinuationPage))
        .flatMap(_.remove(MonthlyFinalValidationSourcePage))

        // monthly return journey origin
        .flatMap(_.remove(IsResumedMonthlyReturnPage))

        // ChRIS submission
        .flatMap(_.remove(SubmissionDetailsPage))
        .flatMap(_.remove(PollUrlPage))
        .flatMap(_.remove(PollIntervalPage))
        .flatMap(_.remove(CorrelationIdPage))
        .flatMap(_.remove(LastMessageDatePage))
        .flatMap(_.remove(SubmissionConfirmationCachePage))

      val withSubmissionIdPages = submissionId.fold(clearedAnswers) { id =>
        clearedAnswers
          .flatMap(_.remove(SubmissionStatusTimedOutPage(id)))
          .flatMap(_.remove(SuccessEmailSentPage(id)))
      }

      period.fold(withSubmissionIdPages) { p =>
        withSubmissionIdPages
          .flatMap(_.remove(SubmissionCreatedPage(p)))
          .flatMap(_.remove(CreatedSubmissionIdPage(p)))
          .flatMap(_.remove(SubmissionJourneyCompletedPage(p)))
      }
    }

    // Multi-tab: Clear only the ID-scoped monthly return journey.
    def clearMonthlyReturnJourney(id: String): Try[UserAnswers] = {
      val submissionId = userAnswers.get(SubmissionDetailsPage(id)).map(_.id)
      val period       = userAnswers.get(DateConfirmPaymentsPage(id)).map(d => YearMonth.from(d).toString)

      val clearedAnswers = userAnswers
        // common
        .remove(DateConfirmPaymentsPage(id))
        .flatMap(_.remove(SubmitInactivityRequestPage(id)))
        .flatMap(_.remove(ConfirmationByEmailPage(id)))
        .flatMap(_.remove(EnterYourEmailAddressPage(id)))
        .flatMap(_.remove(ResubmissionIdPage(id)))

        // monthly nil return
        .flatMap(_.remove(NilReturnStatusPage(id)))
        .flatMap(_.remove(DeclarationPage(id)))

        // monthly standard return
        // TODO: Add UUID-scoped standard return cleanup when the standard return journey is implemented.
        // .flatMap(_.remove(SelectedSubcontractorPage.all(id)))
        // .flatMap(_.remove(VerifySubcontractorsPage(id)))
        // .flatMap(_.remove(AllSubcontractorDetailsAdded(id)))
        // .flatMap(_.remove(PaymentDetailsConfirmationPage(id)))
        // .flatMap(_.remove(EmploymentStatusDeclarationPage(id)))
        // .flatMap(_.remove(VerifiedStatusDeclarationPage(id)))
        // .flatMap(_.remove(SubcontractorValidationFailuresPage(id)))

        // amendment-specific state
        // TODO: Add UUID-scoped amendment cleanup when the amendment journey is implemented.
        // .flatMap(_.remove(AmendmentDetailsPage(id)))
        // .flatMap(_.remove(WhichSubcontractorsToAddPage(id)))
        // .flatMap(_.remove(OriginalSubcontractorCountPage(id)))

        // final validation state
        // TODO: Add UUID-scoped final validation cleanup when the standard return journey is implemented.
        // .flatMap(_.remove(FinalValidationDraftIdPage(id)))
        // .flatMap(_.remove(MonthlyFinalValidationContinuationPage(id)))
        // .flatMap(_.remove(MonthlyFinalValidationSourcePage(id)))

        // monthly return journey origin
        .flatMap(_.remove(IsResumedMonthlyReturnPage(id)))

        // ChRIS submission
        .flatMap(_.remove(SubmissionDetailsPage(id)))
        .flatMap(_.remove(PollUrlPage(id)))
        .flatMap(_.remove(PollIntervalPage(id)))
        .flatMap(_.remove(CorrelationIdPage(id)))
        .flatMap(_.remove(LastMessageDatePage(id)))
        .flatMap(_.remove(SubmissionConfirmationCachePage(id)))

        // ID-scoped journey information
        .flatMap(_.remove(CisIdPage(id)))
        .flatMap(_.remove(ContractorNamePage(id)))
        .flatMap(_.remove(ReturnTypePage(id)))
        .flatMap(_.remove(AgentClientDataPage(id)))

      val withSubmissionIdPages = submissionId.fold(clearedAnswers) { submissionId =>
        clearedAnswers
          .flatMap(_.remove(SubmissionStatusTimedOutPage(submissionId)))
          .flatMap(_.remove(SuccessEmailSentPage(submissionId)))
      }

      period.fold(withSubmissionIdPages) { p =>
        withSubmissionIdPages
          .flatMap(_.remove(SubmissionCreatedForFlowPage(id, p)))
          .flatMap(_.remove(CreatedSubmissionIdForFlowPage(id, p)))
          .flatMap(_.remove(SubmissionJourneyCompletedForFlowPage(id, p)))
      }
    }

    def clearAmendedMonthlyStandardReturnJourney: Try[UserAnswers] =
      userAnswers
        // amended monthly standard return
        .remove(EmploymentStatusDeclarationPage)
        .flatMap(_.remove(SelectedSubcontractorPage.all))
        .flatMap(_.remove(VerifySubcontractorsPage))
        .flatMap(_.remove(AllSubcontractorDetailsAdded))
        .flatMap(_.remove(PaymentDetailsConfirmationPage))
        .flatMap(_.remove(VerifiedStatusDeclarationPage))
        .flatMap(_.remove(SubmitInactivityRequestPage))
        .flatMap(_.remove(WhichSubcontractorsToAddPage))
        .flatMap(_.remove(SubcontractorValidationFailuresPage))
        .flatMap(_.remove(SubmissionConfirmationCachePage))

        // final validation state
        .flatMap(_.remove(FinalValidationDraftIdPage))
        .flatMap(_.remove(MonthlyFinalValidationContinuationPage))
        .flatMap(_.remove(MonthlyFinalValidationSourcePage))

        // monthly return journey origin
        .flatMap(_.remove(IsResumedMonthlyReturnPage))

    def isJourneyComplete: Boolean =
      userAnswers.get(ReturnTypePage) match {
        case Some(MonthlyNilReturn) | Some(MonthlyAmendedNilReturn) =>
          val checks = Seq(
            answered(DateConfirmPaymentsPage),
            answered(SubmitInactivityRequestPage),
            emailSatisfied,
            answered(DeclarationPage)
          )

          checks.forall(identity)

        case Some(MonthlyStandardReturn) | Some(MonthlyAmendedStandardReturn) =>
          Seq(
            answered(DateConfirmPaymentsPage),
            allSubcontractorsComplete,
            isTrue(AllSubcontractorDetailsAdded),
            isTrue(PaymentDetailsConfirmationPage),
            answered(EmploymentStatusDeclarationPage),
            answered(VerifiedStatusDeclarationPage),
            answered(SubmitInactivityRequestPage),
            emailSatisfied
          ).forall(identity)

        case None =>
          false
      }

    // Multi-tab: Check whether the UUID-scoped nil return journey is complete.
    def isJourneyComplete(id: String): Boolean =
      userAnswers.get(ReturnTypePage(id)) match {
        case Some(MonthlyNilReturn) | Some(MonthlyAmendedNilReturn) =>
          val checks = Seq(
            answered(DateConfirmPaymentsPage(id)),
            answered(SubmitInactivityRequestPage(id)),
            emailSatisfied(id),
            answered(DeclarationPage(id))
          )

          checks.forall(identity)
        // TODO: Add support for multi-tab standard return journey completion check once implemented.
        case _                                                      =>
          false
      }
  }
}
