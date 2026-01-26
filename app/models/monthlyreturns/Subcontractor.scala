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

package models.monthlyreturns

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class Subcontractor(
  subcontractorId: Long,
  utr: Option[String],
  pageVisited: Option[Int],
  partnerUtr: Option[String],
  crn: Option[String],
  firstName: Option[String],
  nino: Option[String],
  secondName: Option[String],
  surname: Option[String],
  partnershipTradingName: Option[String],
  tradingName: Option[String],
  subcontractorType: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  country: Option[String],
  postCode: Option[String],
  emailAddress: Option[String],
  phoneNumber: Option[String],
  mobilePhoneNumber: Option[String],
  worksReferenceNumber: Option[String],
  createDate: Option[LocalDateTime],
  lastUpdate: Option[LocalDateTime],
  subbieResourceRef: Option[Long],
  matched: Option[String],
  autoVerified: Option[String],
  verified: Option[String],
  verificationNumber: Option[String],
  taxTreatment: Option[String],
  verificationDate: Option[LocalDateTime],
  version: Option[Int],
  updatedTaxTreatment: Option[String],
  lastMonthlyReturnDate: Option[LocalDateTime],
  pendingVerifications: Option[Int]
)

object Subcontractor:
  given format: OFormat[Subcontractor] = Json.format[Subcontractor]
