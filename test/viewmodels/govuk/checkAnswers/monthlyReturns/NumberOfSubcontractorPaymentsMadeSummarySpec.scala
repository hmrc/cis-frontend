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

package viewmodels.govuk.checkAnswers.monthlyReturns

import base.SpecBase
import models.{CheckMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.monthlyreturns.VerifiedStatusDeclarationPage
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.stubMessages
import viewmodels.checkAnswers.monthlyreturns.NumberOfSubcontractorPaymentsMadeSummary

import java.time.Instant

class NumberOfSubcontractorPaymentsMadeSummarySpec extends SpecBase with Matchers {

  private implicit val messages: Messages = stubMessages()

  private val now: Instant = Instant.now

  private def uaWithSubcontractors(subs: (Int, JsObject)*): UserAnswers =
    UserAnswers(
      id = userAnswersId,
      data = Json.obj(
        "cisId"               -> "1",
        "dateConfirmPayments" -> "2025-10-01",
        "subcontractors"      -> JsObject(subs.map { case (i, o) => i.toString -> o })
      ),
      lastUpdated = now
    )

  private def completeSub(id: Long, name: String): JsObject =
    Json.obj(
      "id"                -> id,
      "name"              -> name,
      "totalPaymentsMade" -> 1000.00,
      "costOfMaterials"   -> 200.00,
      "totalTaxDeducted"  -> 200.00
    )

  "NumberOfSubcontractorPaymentsMadeSummary" - {

    "must return a SummaryListRow with the number of subcontractors" in {

      val answers = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd"),
        2 -> completeSub(1002L, "Another Ltd")
      )

      val result = NumberOfSubcontractorPaymentsMadeSummary.row(answers).value

      result.key.content.asHtml.toString must include(
        messages("monthlyreturns.numberOfSubcontractorPaymentsMade.checkYourAnswersLabel")
      )

      result.value.content.asHtml.toString must include("2")
    }

    "must return a SummaryListRow with one subcontractor when there is one subcontractor" in {

      val answers = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      )

      val result = NumberOfSubcontractorPaymentsMadeSummary.row(answers).value

      result.value.content.asHtml.toString must include("1")
    }

    "must return None when there are no subcontractors" in {

      val answers = uaWithSubcontractors()

      val result = NumberOfSubcontractorPaymentsMadeSummary.row(answers)

      result mustBe None
    }

    "must contain the correct change action" in {

      val answers = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      )

      val result = NumberOfSubcontractorPaymentsMadeSummary.row(answers).value
      val action = result.actions.value.items.head

      action.href mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
        .onPageLoad(CheckMode)
        .url
    }

    "must contain the correct visually hidden text" in {

      val answers = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      )
        .set(VerifiedStatusDeclarationPage, true)
        .success
        .value

      val result = NumberOfSubcontractorPaymentsMadeSummary.row(answers).value
      val action = result.actions.value.items.head
      action.visuallyHiddenText.value mustBe
        messages("monthlyreturns.numberOfSubcontractorPaymentsMade.hidden")
    }
  }
}
