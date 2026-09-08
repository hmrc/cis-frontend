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

package services

import models.monthlyreturns.Subcontractor
import models.validation.{AddressDetails, SubcontractorValidationFailure}
import utils.CommonDetailsValidator

import javax.inject.{Inject, Singleton}

@Singleton
class SubcontractorDetailsValidator @Inject() () {

  def validate(subcontractors: Seq[Subcontractor]): List[SubcontractorValidationFailure] =
    subcontractors.toList.flatMap { subcontractor =>
      val failedFields =
        CommonDetailsValidator.validate(
          emailAddress = subcontractor.emailAddress,
          phoneNumber = subcontractor.phoneNumber,
          mobilePhoneNumber = subcontractor.mobilePhoneNumber,
          address = Some(toAddressDetails(subcontractor))
        )

      Option.when(failedFields.nonEmpty) {
        SubcontractorValidationFailure(
          subcontractorId = subcontractor.subcontractorId,
          failedFields = failedFields
        )
      }
    }

  private def toAddressDetails(subcontractor: Subcontractor): AddressDetails =
    AddressDetails(
      addressLine1 = subcontractor.addressLine1,
      addressLine2 = subcontractor.addressLine2,
      addressLine3 = subcontractor.addressLine3,
      addressLine4 = subcontractor.addressLine4,
      postcode = subcontractor.postCode,
      country = subcontractor.country
    )
}
