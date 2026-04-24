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

import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.UserAnswers
import pages.QuestionPage
import pages.monthlyreturns.*
import play.api.libs.json.Reads

import scala.util.Try

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

    def clearMonthlyReturnJourney: Try[UserAnswers] =
      userAnswers
        // common
        .remove(DateConfirmPaymentsPage)
        .flatMap(_.remove(SubmitInactivityRequestPage))
        .flatMap(_.remove(ConfirmationByEmailPage))
        .flatMap(_.remove(EnterYourEmailAddressPage))

        // monthly nil return
        .flatMap(_.remove(ConfirmEmailAddressPage))
        .flatMap(_.remove(DeclarationPage))

        // monthly standard return
        .flatMap(_.remove(SelectedSubcontractorPage.all))
        .flatMap(_.remove(VerifySubcontractorsPage))
        .flatMap(_.remove(SubcontractorDetailsAddedPage))
        .flatMap(_.remove(PaymentDetailsConfirmationPage))
        .flatMap(_.remove(EmploymentStatusDeclarationPage))
        .flatMap(_.remove(VerifiedStatusDeclarationPage))

    def isJourneyComplete: Boolean =
      userAnswers.get(ReturnTypePage) match {
        case Some(MonthlyNilReturn) =>
          val checks = Seq(
            answered(DateConfirmPaymentsPage),
            answered(SubmitInactivityRequestPage),
            answered(ConfirmationByEmailPage),
            emailSatisfied,
            answered(DeclarationPage)
          )

          checks.forall(identity)

        case Some(MonthlyStandardReturn) =>
          Seq(
            answered(DateConfirmPaymentsPage),
            allSubcontractorsComplete,
            isTrue(SubcontractorDetailsAddedPage),
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
