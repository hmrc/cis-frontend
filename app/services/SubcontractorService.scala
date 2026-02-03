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

import models.monthlyreturns.SelectSubcontractorsPageModel
import services.SubcontractorService.{TAX_YEAR_START_DAY, TAX_YEAR_START_MONTH}
import uk.gov.hmrc.http.HeaderCarrier
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
    today: LocalDate = LocalDate.now()
  )(implicit hc: HeaderCarrier): Future[SelectSubcontractorsPageModel] =
    monthlyReturnService.retrieveMonthlyReturnForEditDetails(cisId, taxMonth, taxYear).map { data =>

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

          val viewModel = SelectSubcontractorsViewModel(
            id = subcontractor.subcontractorId.toInt,
            name = subcontractor.displayName.getOrElse("No name provided"),
            verificationRequired = if (required) "Yes" else "No",
            verificationNumber = subcontractor.verificationNumber.getOrElse("Unknown"),
            taxTreatment = subcontractor.taxTreatment.getOrElse("Unknown")
          )

          (viewModel, includedLastMonth)
        }

      val (subcontractorViewModels, includedLastMonthFlags) = rows.unzip

      val initiallySelectedIds: Seq[Int] = defaultSelection match {
        case Some(true)  => subcontractorViewModels.map(_.id)
        case Some(false) => Seq.empty
        case None        =>
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

object SubcontractorService {
  private val TAX_YEAR_START_MONTH = 4
  private val TAX_YEAR_START_DAY   = 6
}
