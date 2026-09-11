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
import play.api.libs.json.*
import models.finalvalidation.FinalValidationField.*

class FinalValidationFieldSpec extends SpecBase {

  "FinalValidationField" - {

    "return the correct field from its key" in {
      val fields = Seq(
        "tradingName"            -> TradingName,
        "partnershipTradingName" -> PartnershipTradingName,
        "utr"                    -> Utr,
        "partnerUtr"             -> PartnerUtr,
        "crn"                    -> Crn,
        "firstName"              -> FirstName,
        "secondName"             -> SecondName,
        "surname"                -> Surname,
        "nino"                   -> Nino,
        "worksReferenceNumber"   -> WorksReferenceNumber,
        "addressLine1"           -> AddressLine1,
        "addressLine2"           -> AddressLine2,
        "addressLine3"           -> AddressLine3,
        "addressLine4"           -> AddressLine4,
        "country"                -> Country,
        "postCode"               -> PostCode,
        "emailAddress"           -> EmailAddress,
        "phoneNumber"            -> PhoneNumber,
        "mobilePhoneNumber"      -> MobilePhoneNumber
      )

      fields.foreach { case (key, field) =>
        fromKey(key) mustBe Some(field)
      }
    }

    "serialise and deserialise" in {
      Json.toJson[FinalValidationField](Utr) mustBe JsString("utr")

      Json.fromJson[FinalValidationField](JsString("utr")) mustBe JsSuccess(Utr)
    }

    "reject an unknown field" in {
      Json.fromJson[FinalValidationField](JsString("unknown")) mustBe a[JsError]
    }

    "reject a non-string value" in {
      Json.fromJson[FinalValidationField](Json.obj()) mustBe a[JsError]
    }
  }
}
