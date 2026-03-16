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

package models.submission

import play.api.libs.json.{Format, Json}

case class ChrisStandardDeclarations(
  employmentStatus: String,
  verification: String
)

object ChrisStandardDeclarations {
  given format: Format[ChrisStandardDeclarations] = Json.format[ChrisStandardDeclarations]
}

case class ChrisPersonName(
  first: String,
  middle: Option[String],
  last: String
)

object ChrisPersonName {
  given format: Format[ChrisPersonName] = Json.format[ChrisPersonName]
}

case class ChrisStandardSubcontractor(
  subcontractorType: SubcontractorType,
  name: Option[ChrisPersonName],
  tradingName: Option[String],
  partnershipTradingName: Option[String],
  utr: Option[String],
  crn: Option[String],
  nino: Option[String],
  verificationNumber: Option[String],
  totalPayments: Option[BigDecimal],
  costOfMaterials: Option[BigDecimal],
  totalDeducted: Option[BigDecimal]
)

object ChrisStandardSubcontractor {
  given format: Format[ChrisStandardSubcontractor] = Json.format[ChrisStandardSubcontractor]
}

case class ChrisStandardMonthlyReturn(
  subcontractors: Seq[ChrisStandardSubcontractor],
  declarations: ChrisStandardDeclarations
)

object ChrisStandardMonthlyReturn {
  given format: Format[ChrisStandardMonthlyReturn] = Json.format[ChrisStandardMonthlyReturn]
}
