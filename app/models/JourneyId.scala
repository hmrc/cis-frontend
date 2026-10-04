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

package models

import models.JourneyId.SEP
import models.requests.CisPath

/** This 3-part ID allows users, with multiple tabs, to simultaneously manage multiple:
  *   - journey types, e.g. monthly returns, final validations
  *   - resources, e.g. monthly returns of multiple Agent clients, final validations of multiple subcontractors
  * @param userId
  *   provided by Government Gateway
  * @param journeyType
  *   "MonthlyReturn", "FinalValidation", for example
  * @param target
  *   for monthly returns, this is a [[CisPath]]; for final validations, a subcontractor ID
  */
final case class JourneyId(userId: String, journeyType: String, target: String) {
  def asString: String = s"$userId$SEP$cisPath$SEP$target"

  /** @throws NoSuchElementException for non-monthly-return journeys */
  def cisPath: CisPath = CisPath.from(target).get
}
object JourneyId {
  private val SEP = '/'

  def from(str: String): JourneyId =
    val parts = str.split(SEP)
    apply(parts.head, parts(1), parts(2))
}
