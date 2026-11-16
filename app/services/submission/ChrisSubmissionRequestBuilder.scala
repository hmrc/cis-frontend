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

import models.ReturnType.*
import models.monthlyreturns.{CisTaxpayer, GetAllMonthlyReturnDetailsResponse}
import models.submission.*
import models.{ReturnType, UserAnswers}
import pages.monthlyreturns.*
import utils.Normalise.yesNo

import java.time.YearMonth
import javax.inject.Inject

class ChrisSubmissionRequestBuilder @Inject() {

  def build(
    ua: UserAnswers,
    taxpayer: CisTaxpayer,
    isAgent: Boolean,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse,
    isResubmission: Boolean
  ): ChrisSubmissionRequest = {
    val returnType         = ua.get(ReturnTypePage).getOrElse(throw new RuntimeException("ReturnType missing"))
    val common             = buildCommon(ua, taxpayer, isAgent, monthlyReturn)
    val informationCorrect = true
    val inactivityBool     = ua.get(SubmitInactivityRequestPage).contains(true)

    returnType match {
      case MonthlyNilReturn | MonthlyAmendedNilReturn =>
        ChrisSubmissionRequest.fromNil(
          common = common,
          informationCorrect = informationCorrect,
          inactivity = inactivityBool,
          isResubmission = isResubmission
        )

      case MonthlyStandardReturn | MonthlyAmendedStandardReturn =>
        ChrisSubmissionRequest.fromStandard(
          common = common,
          informationCorrect = informationCorrect,
          inactivity = inactivityBool,
          standard = buildStandardMonthlyReturn(ua, monthlyReturn),
          isResubmission = isResubmission
        )
    }
  }

  // Multi-tab: Build the ChRIS submission request from ID-scoped answers.
  def build(
    ua: UserAnswers,
    id: String,
    taxpayer: CisTaxpayer,
    isAgent: Boolean,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse,
    isResubmission: Boolean
  ): ChrisSubmissionRequest = {

    val returnType         = ua.get(ReturnTypePage(id)).getOrElse(throw new RuntimeException("ReturnType missing"))
    val common             = buildCommon(ua, id, taxpayer, isAgent, monthlyReturn)
    val informationCorrect = true
    val inactivityBool     = ua.get(SubmitInactivityRequestPage(id)).contains(true)

    returnType match {
      case MonthlyNilReturn | MonthlyAmendedNilReturn =>
        ChrisSubmissionRequest.fromNil(
          common = common,
          informationCorrect = informationCorrect,
          inactivity = inactivityBool,
          isResubmission = isResubmission
        )

      case MonthlyStandardReturn | MonthlyAmendedStandardReturn =>
        ChrisSubmissionRequest.fromStandard(
          common = common,
          informationCorrect = informationCorrect,
          inactivity = inactivityBool,
          standard = buildStandardMonthlyReturn(ua, id, monthlyReturn),
          isResubmission = isResubmission
        )
    }
  }

  private def buildCommon(
    ua: UserAnswers,
    taxpayer: CisTaxpayer,
    isAgent: Boolean,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse
  ): ChrisSubmissionCommon = {

    val schemeUtr = monthlyReturn.scheme.headOption
      .flatMap(_.utr)
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("Scheme UTR missing"))

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

    val ym = ua
      .get(DateConfirmPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month and year of return missing"))

    val emailOpt = ua.get(EnterYourEmailAddressPage)

    ChrisSubmissionCommon(
      utr = schemeUtr,
      aoReference = accountsOfficeRef,
      monthYear = ym,
      email = emailOpt,
      isAgent = isAgent,
      clientTaxOfficeNumber = taxpayer.taxOfficeNumber,
      clientTaxOfficeRef = taxpayer.taxOfficeRef
    )
  }

  // Multi-tab: Build common ChRIS details using ID-scoped answers.
  private def buildCommon(
    ua: UserAnswers,
    id: String,
    taxpayer: CisTaxpayer,
    isAgent: Boolean,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse
  ): ChrisSubmissionCommon = {

    val schemeUtr = monthlyReturn.scheme.headOption
      .flatMap(_.utr)
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("Scheme UTR missing"))

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

    val accountsOfficeRef: String =
      List(aoDistrict, aoPayType, aoCheckCode, aoReference).flatten.mkString

    val ym = ua
      .get(DateConfirmPaymentsPage(id))
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month and year of return missing"))

    val emailOpt = ua.get(EnterYourEmailAddressPage(id))

    ChrisSubmissionCommon(
      utr = schemeUtr,
      aoReference = accountsOfficeRef,
      monthYear = ym,
      email = emailOpt,
      isAgent = isAgent,
      clientTaxOfficeNumber = taxpayer.taxOfficeNumber,
      clientTaxOfficeRef = taxpayer.taxOfficeRef
    )
  }

  private def buildStandardMonthlyReturn(
    ua: UserAnswers,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse
  ): ChrisStandardMonthlyReturn = {
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

    val subcontractors = StandardReturnSubcontractorsBuilder.build(ua, monthlyReturn.subcontractors)
    ChrisStandardMonthlyReturn(subcontractors, declarations)
  }

  // Multi-tab: Build standard monthly return details from ID-scoped answers.
  private def buildStandardMonthlyReturn(
    ua: UserAnswers,
    id: String,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse
  ): ChrisStandardMonthlyReturn = {

    val employmentStatus: String = ua
      .get(EmploymentStatusDeclarationPage(id))
      .map(yesNo)
      .getOrElse(throw new RuntimeException("Employment status declaration missing"))

    val verification: String = ua
      .get(VerifiedStatusDeclarationPage(id))
      .map(yesNo)
      .getOrElse(throw new RuntimeException("Verification answer missing"))

    val declarations = ChrisStandardDeclarations(
      employmentStatus = employmentStatus,
      verification = verification
    )

    val subcontractors = StandardReturnSubcontractorsBuilder.build(
      ua,
      id,
      monthlyReturn.subcontractors
    )

    ChrisStandardMonthlyReturn(subcontractors, declarations)
  }
}
