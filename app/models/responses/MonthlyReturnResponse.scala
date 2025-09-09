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

package models.responses

import models.MonthlyReturnDetails
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class InTransitMonthlyReturnDetails(
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
) {
  val toMonthlyReturnDetails: MonthlyReturnDetails = MonthlyReturnDetails(
    monthlyReturnId = monthlyReturnId.toString,
    taxYear = taxYear.toString,
    taxMonth = taxMonth.toString,
    nilReturnIndicator = nilReturnIndicator,
    status = status
  )
}

object InTransitMonthlyReturnDetails {
  implicit val format: OFormat[InTransitMonthlyReturnDetails] = Json.format[InTransitMonthlyReturnDetails]
}

case class MonthlyReturnResponse(monthlyReturnList: Seq[InTransitMonthlyReturnDetails])

object MonthlyReturnResponse {
  import InTransitMonthlyReturnDetails.format
  implicit val format: OFormat[MonthlyReturnResponse] = Json.format[MonthlyReturnResponse]
}
