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
import play.api.libs.json.{JsObject, Json}

object SubcontractorDetailsAddedBuilder {

  private def subcontractorsObject(ua: UserAnswers): JsObject =
    (ua.data \ "subcontractors").asOpt[JsObject].getOrElse(Json.obj())

  private def selectedIndexes(ua: UserAnswers): Seq[Int] =
    subcontractorsObject(ua).keys.flatMap(_.toIntOption).filter(_ > 0).toSeq.sorted

  private def subcontractorAt(ua: UserAnswers, index: Int): Option[JsObject] =
    subcontractorsObject(ua).value.get(index.toString).collect { case o: JsObject => o }

  private def subcontractorId(sub: JsObject): Long =
    (sub \ "subcontractorId").toOption
      .flatMap(_.asOpt[Long])
      .orElse((sub \ "id").toOption.flatMap(_.asOpt[Long]))
      .getOrElse(0L)

  private def subcontractorName(sub: JsObject): String =
    (sub \ "name").toOption.flatMap(_.asOpt[String]).getOrElse("")

  private def detailsAdded(sub: JsObject): Boolean = {
    val hasPaymentsMade = (sub \ "totalPaymentsMade").toOption.isDefined
    val hasMaterials    = (sub \ "costOfMaterials").toOption.isDefined
    val hasTaxDeducted  = (sub \ "totalTaxDeducted").toOption.isDefined

    hasPaymentsMade && hasMaterials && hasTaxDeducted
  }

  private def headingKeyAndArgs(addedCount: Int): (String, Seq[AnyRef]) =
    if (addedCount == 1) {
      ("monthlyreturns.subcontractorDetailsAdded.heading.single", Seq.empty)
    } else {
      ("monthlyreturns.subcontractorDetailsAdded.heading.multiple", Seq(addedCount.asInstanceOf[AnyRef]))
    }

  def build(ua: UserAnswers): Option[SubcontractorDetailsAddedViewModel] = {

    val indexes = selectedIndexes(ua)

    if (indexes.isEmpty) {
      None
    } else {
      val rows: Seq[SubcontractorDetailsAddedRow] =
        indexes.flatMap { index =>
          subcontractorAt(ua, index).map { sub =>

            val id    = subcontractorId(sub)
            val name  = subcontractorName(sub)
            val added = detailsAdded(sub)

            SubcontractorDetailsAddedRow(
              index = index,
              subcontractorId = id,
              name = name,
              detailsAdded = added,
              changeCall = controllers.monthlyreturns.routes.PaymentDetailsController
                .onPageLoad(CheckMode, index),
              removeCall = controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController
                .onPageLoad(CheckMode) // index to be added
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
