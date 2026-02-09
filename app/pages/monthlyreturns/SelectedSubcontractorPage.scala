/*
 * Copyright 2025 HM Revenue & Customs
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

package pages.monthlyreturns

import models.monthlyreturns.SelectedSubcontractor
import pages.{IndexedQuestionPage, QuestionPage}
import play.api.libs.json.JsPath

case class SelectedSubcontractorIdPage(index: Int) extends QuestionPage[Long] {
  override def path: JsPath     = JsPath \ "subcontractors" \ index \ toString
  override def toString: String = "subcontractorId"
}

case class SelectedSubcontractorNamePage(index: Int) extends QuestionPage[String] {
  override def path: JsPath     = JsPath \ "subcontractors" \ index \ toString
  override def toString: String = "name"
}

case class SelectedSubcontractorPaymentsMadePage(index: Int) extends QuestionPage[Double] {
  override def path: JsPath     = JsPath \ "subcontractors" \ index \ toString
  override def toString: String = "totalPaymentsMade"
}

case class SelectedSubcontractorMaterialCostsPage(index: Int) extends QuestionPage[Double] {
  override def path: JsPath     = JsPath \ "subcontractors" \ index \ toString
  override def toString: String = "costOfMaterials"
}

case class SelectedSubcontractorTaxDeductedPage(index: Int) extends QuestionPage[Double] {
  override def path: JsPath     = JsPath \ "subcontractors" \ index \ toString
  override def toString: String = "totalTaxDeducted"
}

case class SelectedSubcontractorPage(index: Int) extends QuestionPage[SelectedSubcontractor] {
  override def path: JsPath     = JsPath \ "subcontractors" \ index
  override def toString: String = s"subcontractor-$index"
}

object SelectedSubcontractorPage {
  def all: IndexedQuestionPage[SelectedSubcontractor] = new IndexedQuestionPage[SelectedSubcontractor] {
    override def path: JsPath     = JsPath \ toString
    override def toString: String = "subcontractors"
  }
}
