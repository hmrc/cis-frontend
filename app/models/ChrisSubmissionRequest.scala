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

package models

import play.api.libs.json.{Json, OFormat}

import java.time.YearMonth

case class ChrisSubmissionRequest(
                                   utr: String,
                                   aoReference: String,
                                   informationCorrect: String,
                                   inactivity: String,
                                   monthYear: String
                                 )

object ChrisSubmissionRequest {
  implicit val format: OFormat[ChrisSubmissionRequest] = Json.format[ChrisSubmissionRequest]

  private def yesNo(b: Boolean): String   = if (b) "yes" else "no"
  private def yyMm(ym: YearMonth): String = ym.toString

  def from(
            utr: String,
            aoReference: String,
            informationCorrect: Boolean,
            inactivity: Boolean,
            period: YearMonth
          ): ChrisSubmissionRequest =
    ChrisSubmissionRequest(
      utr = utr,
      aoReference = aoReference,
      informationCorrect = yesNo(informationCorrect),
      inactivity = yesNo(inactivity),
      monthYear = yyMm(period)
    )
}
