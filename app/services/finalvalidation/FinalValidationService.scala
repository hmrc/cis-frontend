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

package services.finalvalidation

import models.finalvalidation.FinalValidationField.*
import models.finalvalidation.*
import models.monthlyreturns.Subcontractor
import services.SubcontractorValidator
import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Try}

@Singleton
class FinalValidationService @Inject() (
  subcontractorValidator: SubcontractorValidator
) extends Logging {

  def validate(
    subcontractors: Seq[Subcontractor]
  ): FinalValidationResult = {

    val failedFieldsBySubcontractor =
      subcontractorValidator.validateFields(
        subcontractors
      )

    val failures =
      subcontractors.flatMap { subcontractor =>

        val fields =
          failedFieldsBySubcontractor
            .getOrElse(
              subcontractor.subcontractorId,
              Seq.empty
            )
            .map(
              FinalValidationFieldMapper.fromValidationField
            )
            .distinct

        Option.when(fields.nonEmpty) {
          SubcontractorFinalValidationFailure(
            subcontractorId = subcontractor.subcontractorId,
            issues = fields.map { field =>
              FinalValidationIssue(
                field = field,
                value = field.valueFrom(subcontractor)
              )
            },
            subbieResourceRef = subcontractor.subbieResourceRef
          )
        }
      }

    FinalValidationResult(
      failures = failures
    )
  }

  def validateDraftSubcontractor(
    draft: FinalValidationDraft,
    subcontractorId: Long
  ): Try[Seq[FinalValidationDraftIssue]] =
    draft.subcontractor(subcontractorId) match {

      case Some(subcontractor) =>
        Try {
          val proposedSubcontractors =
            draft.subcontractors.map(
              toSubcontractor
            )

          val fields =
            subcontractorValidator
              .validateFieldsFor(
                subcontractorId = subcontractorId,
                subcontractors = proposedSubcontractors
              )
              .map(
                FinalValidationFieldMapper.fromValidationField
              )
              .distinct

          fields.map { field =>
            FinalValidationDraftIssue(
              fieldKey = field.key,
              value = valueFor(
                field = field,
                details = subcontractor.proposed
              )
            )
          }
        }

      case None =>
        Failure(
          new IllegalStateException(
            s"Subcontractor $subcontractorId not found in Final Validation draft"
          )
        )
    }

  private def toSubcontractor(
    subcontractor: FinalValidationDraftSubcontractor
  ): Subcontractor = {

    val proposed =
      subcontractor.proposed

    Subcontractor(
      subcontractorId = subcontractor.subcontractorId,
      utr = proposed.utr,
      pageVisited = None,
      partnerUtr = proposed.partnerUtr,
      crn = proposed.crn,
      firstName = proposed.firstName,
      nino = proposed.nino,
      secondName = proposed.secondName,
      surname = proposed.surname,
      partnershipTradingName = proposed.partnershipTradingName,
      tradingName = proposed.tradingName,
      subcontractorType = subcontractor.subcontractorType,
      addressLine1 = proposed.addressLine1,
      addressLine2 = proposed.addressLine2,
      addressLine3 = proposed.addressLine3,
      addressLine4 = proposed.addressLine4,
      country = proposed.country,
      postCode = proposed.postcode,
      emailAddress = proposed.emailAddress,
      phoneNumber = proposed.phoneNumber,
      mobilePhoneNumber = proposed.mobilePhoneNumber,
      worksReferenceNumber = proposed.worksReferenceNumber,
      createDate = None,
      lastUpdate = None,
      subbieResourceRef = None,
      matched = None,
      autoVerified = None,
      verified = None,
      verificationNumber = None,
      taxTreatment = None,
      verificationDate = None,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = None,
      pendingVerifications = None,
      displayName = None
    )
  }

  private def valueFor(
    field: FinalValidationField,
    details: FinalValidationSubcontractorDetails
  ): Option[String] =
    field match {
      case FirstName              => details.firstName
      case SecondName             => details.secondName
      case Surname                => details.surname
      case TradingName            => details.tradingName
      case PartnershipTradingName => details.partnershipTradingName
      case Utr                    => details.utr
      case PartnerUtr             => details.partnerUtr
      case Nino                   => details.nino
      case Crn                    => details.crn
      case AddressLine1           => details.addressLine1
      case AddressLine2           => details.addressLine2
      case AddressLine3           => details.addressLine3
      case AddressLine4           => details.addressLine4
      case Country                => details.country
      case PostCode               => details.postcode
      case EmailAddress           => details.emailAddress
      case PhoneNumber            => details.phoneNumber
      case MobilePhoneNumber      => details.mobilePhoneNumber
      case WorkReferenceNumber    => details.worksReferenceNumber
      case _                      => None
    }
}
