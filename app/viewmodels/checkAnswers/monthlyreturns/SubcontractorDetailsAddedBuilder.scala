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

import models.monthlyreturns.SelectedSubcontractor
import models.{CheckMode, UserAnswers}
import pages.monthlyreturns.SelectedSubcontractorPage

object SubcontractorDetailsAddedBuilder {

  private def selectedSubcontractors(ua: UserAnswers): Map[Int, SelectedSubcontractor] =
    ua.get(SelectedSubcontractorPage.all).getOrElse(Map.empty)

  private def detailsAdded(sub: SelectedSubcontractor): Boolean =
    sub.totalPaymentsMade.isDefined && sub.costOfMaterials.isDefined && sub.totalTaxDeducted.isDefined

  private def headingKeyAndArgs(addedCount: Int): (String, Seq[AnyRef]) =
    if (addedCount == 1) {
      ("monthlyreturns.subcontractorDetailsAdded.heading.single", Seq.empty)
    } else {
      ("monthlyreturns.subcontractorDetailsAdded.heading.multiple", Seq(addedCount.asInstanceOf[AnyRef]))
    }

  def build(ua: UserAnswers): Option[SubcontractorDetailsAddedViewModel] = {

    val subcontractorByIndex = selectedSubcontractors(ua)
    val indexes              = subcontractorByIndex.keys.toSeq.sorted

    if (indexes.isEmpty) {
      None
    } else {
      val rows: Seq[SubcontractorDetailsAddedRow] =
        indexes.flatMap { index =>
          subcontractorByIndex.get(index).map { sub =>

            val added = detailsAdded(sub)

            SubcontractorDetailsAddedRow(
              index = index,
              subcontractorId = sub.id,
              name = sub.name,
              detailsAdded = added,
              changeCall = controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController
                .onPageLoad(index),
              removeCall = controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController
                .onPageLoad(CheckMode) // TODO: add index to the route when implemented
            )
          }
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
  }
}
