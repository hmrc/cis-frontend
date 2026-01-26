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

import java.time.Instant

case class ContractorScheme(
  schemeId: Int,
  instanceId: String,
  accountsOfficeReference: String,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  utr: Option[String] = None,
  name: Option[String] = None,
  emailAddress: Option[String] = None,
  displayWelcomePage: Option[String] = None,
  prePopCount: Option[Int] = None,
  prePopSuccessful: Option[String] = None,
  subcontractorCounter: Option[Int] = None,
  verificationBatchCounter: Option[Int] = None,
  lastUpdate: Option[Instant] = None,
  version: Option[Int] = None
)

object ContractorScheme {
  given OFormat[ContractorScheme] = Json.format[ContractorScheme]
}

final case class CreateContractorSchemeParams(
  instanceId: String,
  accountsOfficeReference: String,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  utr: Option[String] = None,
  name: Option[String] = None,
  emailAddress: Option[String] = None,
  displayWelcomePage: Option[String] = None,
  prePopCount: Option[Int] = None,
  prePopSuccessful: Option[String] = None
)

object CreateContractorSchemeParams {
  given OFormat[CreateContractorSchemeParams] = Json.format[CreateContractorSchemeParams]
}

final case class UpdateContractorSchemeParams(
  schemeId: Int,
  instanceId: String,
  accountsOfficeReference: String,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  utr: Option[String] = None,
  name: Option[String] = None,
  emailAddress: Option[String] = None,
  displayWelcomePage: Option[String] = None,
  prePopCount: Option[Int] = None,
  prePopSuccessful: Option[String] = None,
  version: Option[Int] = None
)

object UpdateContractorSchemeParams {
  given OFormat[UpdateContractorSchemeParams] = Json.format[UpdateContractorSchemeParams]
}

final case class CreateSchemeResponse(schemeId: Int)

object CreateSchemeResponse {
  given format: OFormat[CreateSchemeResponse] = Json.format[CreateSchemeResponse]
}
