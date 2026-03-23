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

package models.submission

import models.ReturnType
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import play.api.libs.json.{Json, OFormat}
import utils.Normalise.yesNo

import java.time.YearMonth

case class ChrisSubmissionRequest(
  utr: String,
  aoReference: String,
  informationCorrect: String,
  inactivity: String,
  monthYear: String,
  email: Option[String],
  isAgent: Boolean,
  clientTaxOfficeNumber: String,
  clientTaxOfficeRef: String,
  returnType: ReturnType,
  standard: Option[ChrisStandardMonthlyReturn] = None,
  langCode: String
)

object ChrisSubmissionRequest {
  implicit val format: OFormat[ChrisSubmissionRequest] = Json.format[ChrisSubmissionRequest]

  private def toYearMonthString(yearMonth: YearMonth): String = yearMonth.toString

  def fromNil(
    common: ChrisSubmissionCommon,
    informationCorrect: Boolean,
    inactivity: Boolean,
    langCode: String
  ): ChrisSubmissionRequest =
    ChrisSubmissionRequest(
      utr = common.utr,
      aoReference = common.aoReference,
      informationCorrect = yesNo(informationCorrect),
      inactivity = yesNo(inactivity),
      monthYear = toYearMonthString(common.monthYear),
      email = common.email.map(_.trim),
      isAgent = common.isAgent,
      clientTaxOfficeNumber = common.clientTaxOfficeNumber,
      clientTaxOfficeRef = common.clientTaxOfficeRef,
      returnType = MonthlyNilReturn,
      standard = None,
      langCode = langCode
    )

  def fromStandard(
    common: ChrisSubmissionCommon,
    informationCorrect: Boolean,
    inactivity: Boolean,
    standard: ChrisStandardMonthlyReturn,
    langCode: String
  ): ChrisSubmissionRequest =
    ChrisSubmissionRequest(
      utr = common.utr,
      aoReference = common.aoReference,
      informationCorrect = yesNo(informationCorrect),
      inactivity = yesNo(inactivity),
      monthYear = toYearMonthString(common.monthYear),
      email = common.email.map(_.trim),
      isAgent = common.isAgent,
      clientTaxOfficeNumber = common.clientTaxOfficeNumber,
      clientTaxOfficeRef = common.clientTaxOfficeRef,
      returnType = MonthlyStandardReturn,
      standard = Some(standard),
      langCode = langCode
    )
}
