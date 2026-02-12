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

import play.api.mvc.Call

case class SubcontractorDetailsAddedRow(
  index: Int,
  subcontractorId: Long,
  name: String,
  detailsAdded: Boolean,
  changeCall: Call,
  removeCall: Call
)

case class SubcontractorDetailsAddedViewModel(
  headingKey: String,
  headingArgs: Seq[AnyRef],
  rows: Seq[SubcontractorDetailsAddedRow],
  hasIncomplete: Boolean
) {
  def addedCount: Int = headingArgs.headOption.map(_.toString.toInt).getOrElse(1)
}
