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

package models.monthlyreturns

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class MonthlyReturn(
  monthlyReturnId: Long,
  taxYear: Int,
  taxMonth: Int,
  nilReturnIndicator: Option[String] = None,
  decEmpStatusConsidered: Option[String] = None,
  decAllSubsVerified: Option[String] = None,
  decInformationCorrect: Option[String] = None,
  decNoMoreSubPayments: Option[String] = None,
  decNilReturnNoPayments: Option[String] = None,
  status: Option[String] = None,
  lastUpdate: Option[LocalDateTime] = None,
  amendment: Option[String] = None,
  supersededBy: Option[Long] = None
)

object MonthlyReturn {
  implicit val format: OFormat[MonthlyReturn] = Json.format[MonthlyReturn]
}

case class UserMonthlyReturns(monthlyReturnList: Seq[MonthlyReturn])

object UserMonthlyReturns {
  import MonthlyReturn.format
  implicit val format: OFormat[UserMonthlyReturns] = Json.format[UserMonthlyReturns]
  val empty: UserMonthlyReturns                    = UserMonthlyReturns(Seq.empty)
}
