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

package models.finalvalidation

import models.validation.SubcontractorValidationField

object FinalValidationFieldMapper {

  def fromValidationField(
    field: SubcontractorValidationField
  ): FinalValidationField =
    field match {
      case SubcontractorValidationField.TradingName            => FinalValidationField.TradingName
      case SubcontractorValidationField.PartnershipTradingName => FinalValidationField.PartnershipTradingName
      case SubcontractorValidationField.Utr                    => FinalValidationField.Utr
      case SubcontractorValidationField.PartnerUtr             => FinalValidationField.PartnerUtr
      case SubcontractorValidationField.Crn                    => FinalValidationField.Crn
      case SubcontractorValidationField.FirstName              => FinalValidationField.FirstName
      case SubcontractorValidationField.SecondName             => FinalValidationField.SecondName
      case SubcontractorValidationField.Surname                => FinalValidationField.Surname
      case SubcontractorValidationField.Nino                   => FinalValidationField.Nino
      case SubcontractorValidationField.WorksReferenceNumber   => FinalValidationField.WorksReferenceNumber
      case SubcontractorValidationField.AddressLine1           => FinalValidationField.AddressLine1
      case SubcontractorValidationField.AddressLine2           => FinalValidationField.AddressLine2
      case SubcontractorValidationField.AddressLine3           => FinalValidationField.AddressLine3
      case SubcontractorValidationField.AddressLine4           => FinalValidationField.AddressLine4
      case SubcontractorValidationField.Country                => FinalValidationField.Country
      case SubcontractorValidationField.Postcode               => FinalValidationField.PostCode
      case SubcontractorValidationField.EmailAddress           => FinalValidationField.EmailAddress
      case SubcontractorValidationField.PhoneNumber            => FinalValidationField.PhoneNumber
      case SubcontractorValidationField.MobilePhoneNumber      => FinalValidationField.MobilePhoneNumber
    }
}
