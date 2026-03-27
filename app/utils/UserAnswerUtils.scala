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

import models.UserAnswers
import pages.monthlyreturns.*

import scala.util.Try

object UserAnswerUtils {
  extension (userAnswers: UserAnswers) {
    def firstIncompleteSubcontractorIndex: Int = userAnswers
      .get(SelectedSubcontractorPage.all)
      .getOrElse(Map())
      .filter((_, subcontractor) => !subcontractor.isComplete)
      .keys
      .minOption
      .getOrElse(1)

    def incompleteSubcontractorIds: Seq[Long] = userAnswers
      .get(SelectedSubcontractorPage.all)
      .getOrElse(Map())
      .values
      .toSeq
      .filter(!_.isComplete)
      .map(_.id)

    def clearMonthlyReturnJourney: Try[UserAnswers] =
      userAnswers
        .remove(DateConfirmPaymentsPage)

        // monthly nil return
        .flatMap(_.remove(InactivityRequestPage))
        .flatMap(_.remove(ConfirmationByEmailPage))
        .flatMap(_.remove(EnterYourEmailAddressPage))
        .flatMap(_.remove(DeclarationPage))

        // monthly standard return
        .flatMap(_.remove(SelectedSubcontractorPage.all))
        .flatMap(_.remove(VerifySubcontractorsPage))
        .flatMap(_.remove(SubcontractorDetailsAddedPage))
        .flatMap(_.remove(SubcontractorDetailsAddedPage))
        .flatMap(_.remove(PaymentDetailsConfirmationPage))
        .flatMap(_.remove(EmploymentStatusDeclarationPage))
        .flatMap(_.remove(VerifiedStatusDeclarationPage))
        .flatMap(_.remove(SubmitInactivityRequestPage))
        .flatMap(_.remove(ConfirmEmailAddressPage))
        .flatMap(_.remove(EnterYourEmailAddressPage))
  }
}
