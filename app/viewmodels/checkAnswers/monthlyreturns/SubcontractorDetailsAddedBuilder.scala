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

package viewmodels.checkAnswers.monthlyreturns

import models.{CheckMode, UserAnswers}
import pages.monthlyreturns.*
import play.api.libs.json.JsArray

object SubcontractorDetailsAddedBuilder {

  private def selectedIndexes(ua: UserAnswers): Seq[Int] = {
    val size =
      (ua.data \ "subcontractors").asOpt[JsArray].map(_.value.size).getOrElse(0)
    0.until(size)
  }

  private def detailsAdded(ua: UserAnswers, index: Int): Boolean =
    ua.get(SelectedSubcontractorTotalPaymentMadePage(index)).isDefined &&
      ua.get(SelectedSubcontractorCostOfMaterialsPage(index)).isDefined &&
      ua.get(SelectedSubcontractorTotalTaxDeductedPage(index)).isDefined

  private def headingKeyAndArgs(addedCount: Int): (String, Seq[AnyRef]) =
    if (addedCount == 1) {
      ("monthlyreturns.subcontractorDetailsAdded.heading.single", Seq.empty)
    } else {
      ("monthlyreturns.subcontractorDetailsAdded.heading.multiple", Seq(addedCount.asInstanceOf[AnyRef]))
    }

  def build(ua: UserAnswers): Option[SubcontractorDetailsAddedViewModel] = {
    val rows = selectedIndexes(ua).map { index =>
      val id    = ua.get(SelectedSubcontractorIdPage(index)).getOrElse(0L)
      val name  = ua.get(SelectedSubcontractorNamePage(index)).getOrElse("")
      val added = detailsAdded(ua, index)

      SubcontractorDetailsAddedRow(
        index = index,
        subcontractorId = id,
        name = name,
        detailsAdded = added,
        changeCall =
          controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(CheckMode), // index to be added
        removeCall = controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController.onPageLoad(CheckMode)
      )
    }

    val hasIncomplete = rows.exists(!_.detailsAdded)
    val rowsToDisplay = rows.filter(_.detailsAdded)
    val addedCount    = rowsToDisplay.size

    if (addedCount == 0) {
      None
    } else {
      val (key, args) = headingKeyAndArgs(addedCount)
      Some(
        SubcontractorDetailsAddedViewModel(
          headingKey = key,
          headingArgs = args,
          rows = rowsToDisplay,
          hasIncomplete = hasIncomplete
        )
      )
    }
  }

  def allSelectedHaveDetails(ua: UserAnswers): Boolean =
    selectedIndexes(ua).forall(index => detailsAdded(ua, index))
}
