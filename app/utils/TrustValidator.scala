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

package utils

import models.monthlyreturns.Subcontractor
import models.validation.FieldValidationFailure
import utils.validation.{TradingNameValidator, UtrValidator, WorksReferenceNumberValidator}

object TrustValidator {
  def validate(
    subcontractor: Subcontractor,
    subcontractors: Seq[Subcontractor]
  ): List[FieldValidationFailure] =
    WorksReferenceNumberValidator
      .validate(subcontractor.worksReferenceNumber)
      .toList ++
      UtrValidator
        .validate(subcontractor.utr, subcontractors)
        .toList ++
      TradingNameValidator
        .validate(subcontractor.tradingName)
        .toList
}
