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

import play.api.libs.json.*

sealed trait FinalValidationChangeTarget { def key: String }

object FinalValidationChangeTarget {

  case object SubcontractorName extends FinalValidationChangeTarget { val key = "subcontractorName" }
  case object TradingName extends FinalValidationChangeTarget { val key = "tradingName" }
  case object PartnershipTradingName extends FinalValidationChangeTarget { val key = "partnershipTradingName" }

  case object AddressYesNo extends FinalValidationChangeTarget { val key = "addressYesNo" }
  case object Address extends FinalValidationChangeTarget { val key = "address" }

  case object ContactDetailsYesNo extends FinalValidationChangeTarget { val key = "contactDetailsYesNo" }
  case object EmailAddress extends FinalValidationChangeTarget { val key = "emailAddress" }
  case object PhoneNumber extends FinalValidationChangeTarget { val key = "phoneNumber" }
  case object MobilePhoneNumber extends FinalValidationChangeTarget { val key = "mobilePhoneNumber" }

  case object UtrYesNo extends FinalValidationChangeTarget { val key = "utrYesNo" }
  case object Utr extends FinalValidationChangeTarget { val key = "utr" }
  case object NinoYesNo extends FinalValidationChangeTarget { val key = "ninoYesNo" }
  case object Nino extends FinalValidationChangeTarget { val key = "nino" }
  case object CrnYesNo extends FinalValidationChangeTarget { val key = "crnYesNo" }
  case object Crn extends FinalValidationChangeTarget { val key = "crn" }
  case object PartnerUtrYesNo extends FinalValidationChangeTarget { val key = "partnerUtrYesNo" }
  case object PartnerUtr extends FinalValidationChangeTarget { val key = "partnerUtr" }
  case object WorksReferenceNumberYesNo extends FinalValidationChangeTarget { val key = "worksReferenceNumberYesNo" }
  case object WorksReferenceNumber extends FinalValidationChangeTarget { val key = "worksReferenceNumber" }

  val values: Seq[FinalValidationChangeTarget] =
    Seq(
      SubcontractorName,
      TradingName,
      PartnershipTradingName,
      AddressYesNo,
      Address,
      ContactDetailsYesNo,
      EmailAddress,
      PhoneNumber,
      MobilePhoneNumber,
      UtrYesNo,
      Utr,
      NinoYesNo,
      Nino,
      CrnYesNo,
      Crn,
      PartnerUtrYesNo,
      PartnerUtr,
      WorksReferenceNumberYesNo,
      WorksReferenceNumber
    )

  def fromKey(key: String): Option[FinalValidationChangeTarget] =
    values.find(_.key == key)

  given Format[FinalValidationChangeTarget] = new Format[FinalValidationChangeTarget] {
    override def writes(o: FinalValidationChangeTarget): JsValue             = JsString(o.key)
    override def reads(json: JsValue): JsResult[FinalValidationChangeTarget] =
      json.validate[String].flatMap { key =>
        fromKey(key) match {
          case Some(value) => JsSuccess(value)
          case None        => JsError(s"Invalid FinalValidationChangeTarget key: $key")
        }
      }
  }

}
