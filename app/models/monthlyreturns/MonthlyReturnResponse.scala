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

case class MonthlyReturnDetails(
  monthlyReturnId: Long,
  taxYear: Int,
  taxMonth: Int,
  nilReturnIndicator: Option[String],
  decEmpStatusConsidered: Option[String],
  decAllSubsVerified: Option[String],
  decInformationCorrect: Option[String],
  decNoMoreSubPayments: Option[String],
  decNilReturnNoPayments: Option[String],
  status: Option[String],
  lastUpdate: Option[LocalDateTime],
  amendment: Option[String],
  supersededBy: Option[Long]
)

object MonthlyReturnDetails {
  implicit val format: OFormat[MonthlyReturnDetails] = Json.format[MonthlyReturnDetails]
}

case class MonthlyReturnResponse(monthlyReturnList: Seq[MonthlyReturnDetails])

object MonthlyReturnResponse {
  implicit val format: OFormat[MonthlyReturnResponse] = Json.format[MonthlyReturnResponse]
}
