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

package services.submission

import connectors.ConstructionIndustrySchemeConnector
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.{ReturnType, UserAnswers}
import models.monthlyreturns.{CisTaxpayer, InactivityRequest}
import models.submission.*
import pages.monthlyreturns.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.Normalise.yesNo

import java.time.YearMonth
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChrisSubmissionRequestBuilder @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector
)(implicit ec: ExecutionContext) {

  def build(ua: UserAnswers, taxpayer: CisTaxpayer, isAgent: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[ChrisSubmissionRequest] = {
    val returnType         = ua.get(ReturnTypePage).getOrElse(throw new RuntimeException("ReturnType missing"))
    val common             = buildCommon(ua, taxpayer, isAgent, returnType)
    val informationCorrect = true
    val inactivityBool     = ua.get(InactivityRequestPage).contains(InactivityRequest.Option1)

    returnType match {
      case MonthlyNilReturn =>
        Future.successful(
          ChrisSubmissionRequest.fromNil(
            common = common,
            informationCorrect = informationCorrect,
            inactivity = inactivityBool
          )
        )

      case MonthlyStandardReturn =>
        buildStandardMonthlyReturn(ua).map { standardReturn =>
          ChrisSubmissionRequest.fromStandard(
            common = common,
            informationCorrect = informationCorrect,
            inactivity = inactivityBool,
            standard = standardReturn
          )
        }
    }
  }

  private def buildCommon(
    ua: UserAnswers,
    taxpayer: CisTaxpayer,
    isAgent: Boolean,
    returnType: ReturnType
  ): ChrisSubmissionCommon = {

    val utr = taxpayer.utr
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer UTR missing"))

    val aoDistrict = taxpayer.aoDistrict
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoDistrict missing"))

    val aoPayType = taxpayer.aoPayType
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoPayType missing"))

    val aoCheckCode = taxpayer.aoCheckCode
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoCheckCode missing"))

    val aoReference = taxpayer.aoReference
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoReference missing"))

    // AO reference = aoDistrict + aoPayType + aoCheckCode + aoReference
    val accountsOfficeRef: String =
      List(aoDistrict, aoPayType, aoCheckCode, aoReference).flatten.mkString

    val ym = (returnType match {
      case MonthlyNilReturn      => ua.get(DateConfirmNilPaymentsPage).map(YearMonth.from)
      case MonthlyStandardReturn => ua.get(DateConfirmPaymentsPage).map(YearMonth.from)
    }).getOrElse(throw new RuntimeException("Month and year of return missing"))

    val emailOpt = returnType match {
      case MonthlyNilReturn      => ua.get(ConfirmEmailAddressPage)
      case MonthlyStandardReturn => ua.get(EnterYourEmailAddressPage)
    }

    ChrisSubmissionCommon(
      utr = utr,
      aoReference = accountsOfficeRef,
      monthYear = ym,
      email = emailOpt,
      isAgent = isAgent,
      clientTaxOfficeNumber = taxpayer.taxOfficeNumber,
      clientTaxOfficeRef = taxpayer.taxOfficeRef
    )
  }

  private def buildStandardMonthlyReturn(
    ua: UserAnswers
  )(implicit hc: HeaderCarrier): Future[ChrisStandardMonthlyReturn] = {
    val employmentStatus: String = ua
      .get(EmploymentStatusDeclarationPage)
      .map(yesNo)
      .getOrElse(throw new RuntimeException("Employment status declaration missing"))

    val verification: String = ua
      .get(VerifiedStatusDeclarationPage)
      .map(yesNo)
      .getOrElse(throw new RuntimeException("Verification answer missing"))

    val declarations = ChrisStandardDeclarations(
      employmentStatus = employmentStatus,
      verification = verification
    )

    val instanceId = ua.get(CisIdPage).getOrElse(throw new RuntimeException("CIS ID missing"))
    val ym         = YearMonth.from(ua.get(DateConfirmPaymentsPage).get)

    cisConnector
      .retrieveMonthlyReturnForEditDetails(instanceId, ym.getMonthValue, ym.getYear)
      .map { details =>
        val subcontractors = StandardReturnSubcontractorsBuilder.build(ua, details.subcontractors)
        ChrisStandardMonthlyReturn(subcontractors, declarations)
      }
  }
}
