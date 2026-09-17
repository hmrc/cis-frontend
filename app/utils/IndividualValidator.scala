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
import models.submission.SubcontractorType.SoleTrader
import models.validation.{FieldValidationFailure, SubcontractorValidationField}
import utils.validation.*

object IndividualValidator {

  def validate(
    subcontractorToValidate: Subcontractor,
    allSubcontractors: Seq[Subcontractor]
  ): List[FieldValidationFailure] =
    NameValidator
      .validate(
        firstName = subcontractorToValidate.firstName,
        secondName = subcontractorToValidate.secondName,
        surname = subcontractorToValidate.surname,
        tradingName = subcontractorToValidate.tradingName
      )
      .toList ++
      SurnameValidator
        .validate(subcontractorToValidate.surname)
        .toList ++
      FirstNameValidator
        .validate(subcontractorToValidate.firstName)
        .toList ++
      SecondNameValidator
        .validate(subcontractorToValidate.secondName)
        .toList ++
      TradingNameValidator
        .validate(
          subcontractorToValidate.tradingName,
          SubcontractorValidationField.TradingName,
          SoleTrader
        )
        .toList ++
      UtrValidator
        .validate(
          subcontractorToValidate.utr,
          allSubcontractors
        )
        .toList ++
      NinoValidator
        .validate(subcontractorToValidate.nino)
        .toList ++
      WorksReferenceNumberValidator
        .validate(subcontractorToValidate.worksReferenceNumber)
        .toList
}
