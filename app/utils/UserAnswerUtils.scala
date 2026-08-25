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

    private def emailSatisfied: Boolean = {
      val byEmail = userAnswers.get(ConfirmationByEmailPage)
      byEmail.contains(false) || answered(EnterYourEmailAddressPage)
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

        // ChRIS submission
        .flatMap(_.remove(SubmissionDetailsPage))
        .flatMap(_.remove(PollUrlPage))
        .flatMap(_.remove(PollIntervalPage))
        .flatMap(_.remove(CorrelationIdPage))
        .flatMap(_.remove(LastMessageDatePage))

      val withSubmissionIdPages = submissionId.fold(clearedAnswers) { id =>
        clearedAnswers
          .flatMap(_.remove(SubmissionStatusTimedOutPage(id)))
          .flatMap(_.remove(SuccessEmailSentPage(id)))
      }

      period.fold(withSubmissionIdPages) { p =>
        withSubmissionIdPages
          .flatMap(_.remove(SubmissionCreatedPage(p)))
          .flatMap(_.remove(SubmissionJourneyCompletedPage(p)))
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

    def isJourneyComplete: Boolean =
      userAnswers.get(ReturnTypePage) match {
        case Some(MonthlyNilReturn) | Some(MonthlyAmendedNilReturn) =>
          val checks = Seq(
            answered(DateConfirmPaymentsPage),
            answered(SubmitInactivityRequestPage),
            answered(ConfirmationByEmailPage),
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
            answered(ConfirmationByEmailPage),
            emailSatisfied
          ).forall(identity)

        case None =>
          false
      }
  }
}
