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

package services

import models.UserAnswers
import models.monthlyreturns.SelectSubcontractorsPageModel
import models.requests.GetMonthlyReturnForEditRequest
import pages.amend.AmendmentDetailsPage
import models.amend.{Subcontractor as AmendSubcontractor, WhichSubcontractorsToAddPageModel}
import models.monthlyreturns.Subcontractor
import models.submission.SubcontractorType
import pages.monthlyreturns.SelectedSubcontractorPage
import scala.util.Try
import services.SubcontractorService.{TAX_YEAR_START_DAY, TAX_YEAR_START_MONTH, resolveSubcontractorName}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Normalise.nonBlank
import viewmodels.SelectSubcontractorsViewModel

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubcontractorService @Inject() (monthlyReturnService: MonthlyReturnService)(using ExecutionContext) {

  def verificationPeriodStart(today: LocalDate): LocalDate = {
    val dateTwoYearsAgo       = today.minusYears(2)
    val taxYearCandidate      = dateTwoYearsAgo.getYear
    val taxYearStartCandidate = LocalDate.of(taxYearCandidate, TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY)

    val taxYear =
      if (dateTwoYearsAgo.isBefore(taxYearStartCandidate)) {
        taxYearCandidate - 1
      } else {
        taxYearCandidate
      }

    LocalDate.of(taxYear, TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY)
  }

  def verificationRequired(
    verified: Option[String],
    verificationDate: Option[LocalDateTime],
    lastMonthlyReturnDate: Option[LocalDateTime],
    verificationPeriodStart: LocalDate
  ): Boolean = {
    val isVerified = verified.contains("Y")

    if (!isVerified || verificationDate.isEmpty) {
      true
    } else {
      val verificationDateOk = verificationDate
        .map(_.toLocalDate)
        .exists(date => !date.isBefore(verificationPeriodStart))

      val lastMonthlyReturnOk = lastMonthlyReturnDate
        .map(_.toLocalDate)
        .exists(date => !date.isBefore(verificationPeriodStart))

      !(verificationDateOk || lastMonthlyReturnOk)
    }
  }

  // Build the page model for MR-03-02a

  def buildSelectSubcontractorPage(
    cisId: String,
    taxMonth: Int,
    taxYear: Int,
    defaultSelection: Option[Boolean],
    userAnswers: Option[UserAnswers] = None,
    today: LocalDate = LocalDate.now()
  )(implicit hc: HeaderCarrier): Future[SelectSubcontractorsPageModel] = {
    val isAmendment = userAnswers.exists(_.get(AmendmentDetailsPage).isDefined)
    monthlyReturnService
      .retrieveMonthlyReturnForEditDetails(GetMonthlyReturnForEditRequest(cisId, taxMonth, taxYear, isAmendment))
      .map { data =>

        val previouslyIncludedResourceRefs: Set[Long] =
          data.monthlyReturnItems.flatMap(_.itemResourceReference).toSet

        val periodStart = verificationPeriodStart(today)

        val rows: Seq[(SelectSubcontractorsViewModel, Boolean)] =
          data.subcontractors.map { subcontractor =>
            val includedLastMonth = subcontractor.subbieResourceRef.exists(previouslyIncludedResourceRefs.contains)

            val required = verificationRequired(
              subcontractor.verified,
              subcontractor.verificationDate,
              subcontractor.lastMonthlyReturnDate,
              periodStart
            )

            val verificationNumber: String =
              if (!required) {
                subcontractor.verificationNumber.map(_.trim).filter(_.nonEmpty).getOrElse("Unknown")
              } else {
                "Unknown"
              }

            val taxTreatment: String =
              if (!required && subcontractor.taxTreatment.isDefined) {
                subcontractor.taxTreatment
                  .map(_.trim.toLowerCase)
                  .collect {
                    case "net"       => "Standard rate"
                    case "unmatched" => "Higher rate"
                    case "gross"     => "Gross"
                  }
                  .getOrElse("Unknown")
              } else {
                "Unknown"
              }

            val viewModel = SelectSubcontractorsViewModel(
              id = subcontractor.subcontractorId.toInt,
              name = subcontractor.displayName.getOrElse("No name provided"),
              verificationRequired = if (required) "Yes" else "No",
              verificationNumber = verificationNumber,
              taxTreatment = taxTreatment
            )

            (viewModel, includedLastMonth)
          }

        val (subcontractorViewModels, includedLastMonthFlags) = rows.unzip

        val selectedSubcontractors = userAnswers.flatMap(_.get(SelectedSubcontractorPage.all)).getOrElse(Map())

        val initiallySelectedIds: Seq[Int] = defaultSelection match {
          case Some(true)                              => subcontractorViewModels.map(_.id)
          case Some(false)                             => Seq.empty
          case None if selectedSubcontractors.nonEmpty => selectedSubcontractors.values.map(_.id.toInt).toSeq
          case None                                    =>
            subcontractorViewModels
              .zip(includedLastMonthFlags)
              .collect { case (vm, true) => vm.id }
        }

        SelectSubcontractorsPageModel(
          subcontractors = subcontractorViewModels,
          initiallySelectedIds = initiallySelectedIds
        )
      }
  }

  def buildAmendWhichSubcontractorsPage(
    cisId: String,
    taxMonth: Int,
    taxYear: Int,
    userAnswers: Option[UserAnswers] = None
  )(implicit hc: HeaderCarrier): Future[WhichSubcontractorsToAddPageModel] =
    monthlyReturnService
      .retrieveMonthlyReturnForEditDetails(GetMonthlyReturnForEditRequest(cisId, taxMonth, taxYear, true))
      .map { data =>

        val previouslyIncludedResourceRefs: Set[Long] =
          data.monthlyReturnItems.flatMap(_.itemResourceReference).toSet

        val subcontractors: Seq[AmendSubcontractor] =
          data.subcontractors.map { sub =>
            AmendSubcontractor(
              id = sub.subcontractorId.toString,
              name = resolveSubcontractorName(sub)
            )
          }

        val preSelectedIds: Set[String] =
          userAnswers.flatMap(_.get(pages.amend.WhichSubcontractorsToAddPage)) match {
            case Some(ids) => ids
            case None      =>
              data.subcontractors
                .filter(sub => sub.subbieResourceRef.exists(previouslyIncludedResourceRefs.contains))
                .map(_.subcontractorId.toString)
                .toSet
          }

        val submissionStatus = data.monthlyReturn.headOption.flatMap(_.status)

        WhichSubcontractorsToAddPageModel(
          subcontractors = subcontractors,
          preSelectedIds = preSelectedIds,
          status = submissionStatus
        )
      }

}

object SubcontractorService {
  private val TAX_YEAR_START_MONTH = 4
  private val TAX_YEAR_START_DAY   = 6

  private val NoNameProvided = "No name provided"

  def resolveSubcontractorName(sub: Subcontractor): String =
    sub.subcontractorType.flatMap(t => Try(SubcontractorType.fromString(t)).toOption) match {
      case Some(SubcontractorType.SoleTrader)  =>
        (nonBlank(sub.firstName), nonBlank(sub.surname)) match {
          case (Some(first), Some(last)) => s"$first $last"
          case (_, Some(last))           => last
          case _                         => nonBlank(sub.tradingName).getOrElse(NoNameProvided)
        }
      case Some(SubcontractorType.Company)     =>
        nonBlank(sub.tradingName).getOrElse(NoNameProvided)
      case Some(SubcontractorType.Trust)       =>
        nonBlank(sub.tradingName).getOrElse(NoNameProvided)
      case Some(SubcontractorType.Partnership) =>
        nonBlank(sub.partnershipTradingName)
          .orElse(nonBlank(sub.tradingName))
          .getOrElse(NoNameProvided)
      case _                                   =>
        NoNameProvided
    }
}
