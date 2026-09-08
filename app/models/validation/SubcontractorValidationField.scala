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

package models.validation

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, Reads, Writes}

enum SubcontractorValidationField(val value: String) {
  case EmailAddress extends SubcontractorValidationField("emailAddress")
  case PhoneNumber extends SubcontractorValidationField("phoneNumber")
  case MobilePhoneNumber extends SubcontractorValidationField("mobilePhoneNumber")
  case AddressLine1 extends SubcontractorValidationField("addressLine1")
  case AddressLine2 extends SubcontractorValidationField("addressLine2")
  case AddressLine3 extends SubcontractorValidationField("addressLine3")
  case AddressLine4 extends SubcontractorValidationField("addressLine4")
  case Postcode extends SubcontractorValidationField("postcode")
  case Country extends SubcontractorValidationField("country")
  case WorksReferenceNumber extends SubcontractorValidationField("worksReferenceNumber")
  case Crn extends SubcontractorValidationField("crn")
  case Utr extends SubcontractorValidationField("utr")
  case TradingName extends SubcontractorValidationField("tradingName")
}

object SubcontractorValidationField {

  private def fromString(value: String): JsResult[SubcontractorValidationField] =
    values
      .find(_.value == value)
      .fold[JsResult[SubcontractorValidationField]](
        JsError(s"Unknown subcontractor validation field: $value")
      )(JsSuccess(_))

  given Format[SubcontractorValidationField] = Format(
    Reads {
      case JsString(value) => fromString(value)
      case _               => JsError("Subcontractor validation field must be a string")
    },
    Writes(field => JsString(field.value))
  )
}
