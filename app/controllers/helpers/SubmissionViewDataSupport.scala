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

package controllers.helpers

import models.ReturnType.*
import models.requests.DataRequest
import models.{EmployerReference, ReturnType, UserAnswers}
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.*
import play.api.Logging

import java.time.LocalDate

trait SubmissionViewDataSupport extends Logging {
  protected def formatEmployerRef(er: EmployerReference): String =
    s"${er.taxOfficeNumber}/${er.taxOfficeReference}"

  protected def fail(errorMessage: String): Nothing = {
    logger.error(errorMessage)
    throw new IllegalStateException(errorMessage)
  }

  protected def required[A](opt: Option[A], error: String): A =
    opt.getOrElse(fail(error))

  protected def emailfromUserAnswers(ua: UserAnswers, submissionType: ReturnType): Option[String] = {
    val rawEmail = submissionType match {
      case MonthlyNilReturn      => ua.get(ConfirmEmailAddressPage)
      case MonthlyStandardReturn => ua.get(EnterYourEmailAddressPage)
    }
    rawEmail.map(_.trim).filter(_.nonEmpty)
  }

  protected def periodEndFromUserAnswers(ua: UserAnswers, submissionType: ReturnType): Option[LocalDate] =
    submissionType match {
      case MonthlyNilReturn      => ua.get(DateConfirmNilPaymentsPage)
      case MonthlyStandardReturn => ua.get(DateConfirmPaymentsPage)
    }

  protected def contractorNameFrom(request: DataRequest[_]): String = {
    val ua  = request.userAnswers
    val msg = s"[SubmissionSuccess] contractorName missing for userId=${request.userId}"
    if (!request.isAgent) {
      required(ua.get(ContractorNamePage), msg)
    } else {
      ua.get(AgentClientDataPage).flatMap(_.schemeName).getOrElse(fail(msg))
    }
  }

  protected def employerRefFrom(request: DataRequest[_]): String = {
    val msg = s"[SubmissionSuccess] employerReference missing for userId=${request.userId}"
    if (!request.isAgent) {
      request.employerReference.map(formatEmployerRef).getOrElse(fail(msg))
    } else {
      request.userAnswers
        .get(AgentClientDataPage)
        .filter(_.taxOfficeNumber.nonEmpty)
        .map(data => formatEmployerRef(EmployerReference(data.taxOfficeNumber, data.taxOfficeReference)))
        .getOrElse(fail(msg))
    }
  }
}
