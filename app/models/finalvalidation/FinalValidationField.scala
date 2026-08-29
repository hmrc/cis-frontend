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

import models.monthlyreturns.Subcontractor
import play.api.libs.json.*

sealed trait FinalValidationField {
  def key: String
  def valueFrom(subcontractor: Subcontractor): Option[String]
}

object FinalValidationField {

  case object TradingName extends FinalValidationField {
    override def key: String = "tradingName"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.tradingName
  }

  case object PartnershipTradingName extends FinalValidationField {
    override def key: String = "partnershipTradingName"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.partnershipTradingName
  }

  case object Utr extends FinalValidationField {
    override def key: String = "utr"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.utr
  }

  case object PartnerUtr extends FinalValidationField {
    override def key: String = "partnerUtr"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.partnerUtr
  }

  case object Crn extends FinalValidationField {
    override def key: String = "crn"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.crn
  }

  case object FirstName extends FinalValidationField {
    override def key: String = "firstName"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.firstName
  }

  case object SecondName extends FinalValidationField {
    override def key: String = "secondName"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.secondName
  }

  case object Surname extends FinalValidationField {
    override def key: String = "surname"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.surname
  }

  case object Nino extends FinalValidationField {
    override def key: String = "nino"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.nino
  }

  case object WorkReferenceNumber extends FinalValidationField {
    override def key: String = "workReferenceNumber"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.worksReferenceNumber
  }

  case object AddressLine1 extends FinalValidationField {
    override def key: String = "addressLine1"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.addressLine1
  }

  case object AddressLine2 extends FinalValidationField {
    override def key: String = "addressLine2"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.addressLine2
  }

  case object AddressLine3 extends FinalValidationField {
    override def key: String = "addressLine3"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.addressLine3
  }

  case object AddressLine4 extends FinalValidationField {
    override def key: String = "addressLine4"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.addressLine4
  }

  case object Country extends FinalValidationField {
    override def key: String = "country"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.country
  }

  case object PostCode extends FinalValidationField {
    override def key: String = "postCode"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.postCode
  }

  case object EmailAddress extends FinalValidationField {
    override def key: String = "emailAddress"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.emailAddress
  }

  case object PhoneNumber extends FinalValidationField {
    override def key: String = "phoneNumber"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.phoneNumber
  }

  case object MobilePhoneNumber extends FinalValidationField {
    override def key: String = "mobilePhoneNumber"

    override def valueFrom(subcontractor: Subcontractor): Option[String] = subcontractor.mobilePhoneNumber
  }

  val values: Seq[FinalValidationField] = Seq(
    TradingName,
    PartnershipTradingName,
    Utr,
    PartnerUtr,
    Crn,
    FirstName,
    SecondName,
    Surname,
    Nino,
    WorkReferenceNumber,
    AddressLine1,
    AddressLine2,
    AddressLine3,
    AddressLine4,
    Country,
    PostCode,
    EmailAddress,
    PhoneNumber,
    MobilePhoneNumber
  )

  given Format[FinalValidationField] = new Format[FinalValidationField] {
    override def writes(o: FinalValidationField): JsValue = JsString(o.key)

    override def reads(json: JsValue): JsResult[FinalValidationField] = json match {
      case JsString(key) =>
        values.find(_.key == key) match {
          case Some(field) => JsSuccess(field)
          case None        => JsError(s"Unknown FinalValidationField key: $key")
        }
      case _             => JsError("Expected a string for FinalValidationField")
    }
  }
}
