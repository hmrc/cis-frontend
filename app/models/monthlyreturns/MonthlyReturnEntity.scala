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

case class MonthlyReturnEntity(
  monthlyReturnId: Long,
  schemeId: Long,
  taxYear: Int,
  taxMonth: Int,
  taxYearPrevious: Option[String],
  taxMonthPrevious: Option[String],
  nilReturnIndicator: String,
  decNilReturnNoPayments: String,
  decInformationCorrect: String,
  decNoMoreSubPayments: String,
  decAllSubsVerified: String,
  decEmpStatusConsidered: String,
  status: String,
  createDate: LocalDateTime,
  lastUpdate: LocalDateTime,
  version: Int,
  lMigrated: Option[Long],
  amendment: String,
  supersededBy: Option[Long]
)

object MonthlyReturnEntity {
  implicit val format: OFormat[MonthlyReturnEntity] = Json.format[MonthlyReturnEntity]
}
