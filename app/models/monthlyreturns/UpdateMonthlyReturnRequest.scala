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

package models.monthlyreturns

import models.ReturnType.*
import models.amend.AmendmentDetails
import models.{ReturnType, UserAnswers}
import pages.monthlyreturns.*
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class UpdateMonthlyReturnRequest(
  instanceId: String,
  taxYear: Int,
  taxMonth: Int,
  amendment: String,
  decEmpStatusConsidered: Option[String] = None,
  decAllSubsVerified: Option[String] = None,
  decNoMoreSubPayments: Option[String] = None,
  decNilReturnNoPayments: Option[String] = None,
  decInformationCorrect: Option[String] = None,
  nilReturnIndicator: String,
  status: String,
  version: Option[Long] = None
)

object UpdateMonthlyReturnRequest {

  implicit val format: OFormat[UpdateMonthlyReturnRequest] = Json.format[UpdateMonthlyReturnRequest]

  private val Yes     = "Y"
  private val No      = "N"
  private val Started = "STARTED"

  private def toYN(value: Boolean): String = if (value) "Y" else "N"

  private def dateFor(ua: UserAnswers): Either[String, LocalDate] =
    ua.get(DateConfirmPaymentsPage).toRight("Missing confirmed payment date")

  private def inactivityY(ua: UserAnswers): Option[String] =
    ua.get(SubmitInactivityRequestPage) match {
      case Some(true)  => Some("Y")
      case Some(false) => None
      case None        => None
    }

  private def nilIndicator(returnType: ReturnType): String =
    returnType match {
      case MonthlyNilReturn | MonthlyAmendedNilReturn           => "Y"
      case MonthlyStandardReturn | MonthlyAmendedStandardReturn => "N"
    }

  def fromUserAnswers(ua: UserAnswers): Either[String, UpdateMonthlyReturnRequest] =
    for {
      returnType <- ua.get(ReturnTypePage).toRight("Missing return type")
      instanceId <- ua.get(CisIdPage).toRight("Missing instanceId")
      date       <- dateFor(ua)

      decInformationCorrect = returnType match {
                                case MonthlyNilReturn | MonthlyAmendedNilReturn =>
                                  ua.get(DeclarationPage).flatMap { declaration =>
                                    if (declaration.nonEmpty) Some("Y") else None
                                  }

                                case MonthlyStandardReturn | MonthlyAmendedStandardReturn =>
                                  ua.get(PaymentDetailsConfirmationPage).map(toYN)
                              }
    } yield {
      val base = UpdateMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = date.getYear,
        taxMonth = date.getMonthValue,
        amendment = returnType.amendmentFlag,
        decInformationCorrect = decInformationCorrect,
        nilReturnIndicator = nilIndicator(returnType),
        status = "STARTED",
        version = None
      )

      returnType match {
        case MonthlyStandardReturn | MonthlyAmendedStandardReturn =>
          base.copy(
            decEmpStatusConsidered = ua.get(EmploymentStatusDeclarationPage).map(toYN),
            decAllSubsVerified = ua.get(VerifiedStatusDeclarationPage).map(toYN),
            decNoMoreSubPayments = inactivityY(ua)
          )

        case MonthlyNilReturn | MonthlyAmendedNilReturn =>
          base.copy(
            decNilReturnNoPayments = inactivityY(ua)
          )
      }
    }

  def forStartedStandardAmendment(amendmentDetails: AmendmentDetails): UpdateMonthlyReturnRequest =
    UpdateMonthlyReturnRequest(
      instanceId = amendmentDetails.instanceId,
      taxYear = amendmentDetails.taxYear,
      taxMonth = amendmentDetails.taxMonth,
      amendment = Yes,
      nilReturnIndicator = No,
      status = Started
    )
}
