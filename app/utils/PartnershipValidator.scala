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
import models.submission.SubcontractorType
import models.validation.{FieldValidationFailure, SubcontractorValidationField}
import utils.validation.*

object PartnershipValidator {

  def validate(
    subcontractorToValidate: Subcontractor,
    allSubcontractors: Seq[Subcontractor]
  ): List[FieldValidationFailure] =
    TradingNameValidator
      .validate(
        value = subcontractorToValidate.partnershipTradingName,
        field = SubcontractorValidationField.PartnershipTradingName,
        subcontractorType = SubcontractorType.Partnership
      )
      .toList ++
      UtrValidator
        .validate(
          value = subcontractorToValidate.utr,
          subcontractors = allSubcontractors
        )
        .toList ++
      TradingNameValidator
        .validate(
          value = subcontractorToValidate.tradingName,
          field = SubcontractorValidationField.TradingName,
          subcontractorType = SubcontractorType.Partnership
        )
        .toList ++
      UtrValidator
        .validate(
          value = subcontractorToValidate.partnerUtr,
          subcontractors = allSubcontractors,
          field = SubcontractorValidationField.PartnerUtr,
          checkDuplicate = false
        )
        .toList ++
      NinoValidator
        .validate(subcontractorToValidate.nino)
        .toList ++
      CrnValidator
        .validate(subcontractorToValidate.crn)
        .toList ++
      WorksReferenceNumberValidator
        .validate(subcontractorToValidate.worksReferenceNumber)
        .toList
}
