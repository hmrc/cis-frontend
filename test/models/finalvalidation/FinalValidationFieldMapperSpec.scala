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

import base.SpecBase
import models.validation.SubcontractorValidationField

class FinalValidationFieldMapperSpec extends SpecBase {

  "FinalValidationFieldMapper.fromValidationField" - {

    "map validation fields to final validation fields" in {

      val mappings = Seq(
        SubcontractorValidationField.TradingName            ->
          FinalValidationField.TradingName,
        SubcontractorValidationField.PartnershipTradingName ->
          FinalValidationField.PartnershipTradingName,
        SubcontractorValidationField.Utr                    ->
          FinalValidationField.Utr,
        SubcontractorValidationField.PartnerUtr             ->
          FinalValidationField.PartnerUtr,
        SubcontractorValidationField.Crn                    ->
          FinalValidationField.Crn,
        SubcontractorValidationField.FirstName              ->
          FinalValidationField.FirstName,
        SubcontractorValidationField.SecondName             ->
          FinalValidationField.SecondName,
        SubcontractorValidationField.Surname                ->
          FinalValidationField.Surname,
        SubcontractorValidationField.Nino                   ->
          FinalValidationField.Nino,
        SubcontractorValidationField.WorksReferenceNumber   ->
          FinalValidationField.WorkReferenceNumber,
        SubcontractorValidationField.AddressLine1           ->
          FinalValidationField.AddressLine1,
        SubcontractorValidationField.AddressLine2           ->
          FinalValidationField.AddressLine2,
        SubcontractorValidationField.AddressLine3           ->
          FinalValidationField.AddressLine3,
        SubcontractorValidationField.AddressLine4           ->
          FinalValidationField.AddressLine4,
        SubcontractorValidationField.Country                ->
          FinalValidationField.Country,
        SubcontractorValidationField.Postcode               ->
          FinalValidationField.PostCode,
        SubcontractorValidationField.EmailAddress           ->
          FinalValidationField.EmailAddress,
        SubcontractorValidationField.PhoneNumber            ->
          FinalValidationField.PhoneNumber,
        SubcontractorValidationField.MobilePhoneNumber      ->
          FinalValidationField.MobilePhoneNumber
      )

      mappings.foreach { case (input, expected) =>
        FinalValidationFieldMapper.fromValidationField(input) mustBe expected
      }
    }
  }
}
