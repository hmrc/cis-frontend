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
import models.submission.SubcontractorType
import models.submission.SubcontractorType.{Company, Partnership, SoleTrader, Trust}
import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Try}

@Singleton
class FinalValidationService @Inject() (
  companySubcontractorFinalValidation: CompanySubcontractorFinalValidation,
  individualSubcontractorFinalValidation: IndividualSubcontractorFinalValidation,
  trustSubcontractorFinalValidation: TrustSubcontractorFinalValidation,
  partnershipSubcontractorFinalValidation: PartnershipSubcontractorFinalValidation,
  addressDetailsFinalValidation: AddressDetailsFinalValidation
) extends Logging {

  /** F1 - initial Final Validation.
    *
    * This runs before a Mongo FinalValidationDraft exists, so it uses the monthly-return Subcontractor model.
    */
  def validate(
    selectedSubcontractors: Seq[Subcontractor],
    allSubcontractors: Seq[Subcontractor]
  ): FinalValidationResult = {

    val failures =
      selectedSubcontractors.flatMap { subcontractor =>

        val issues =
          initialValidationIssues(
            subcontractor = subcontractor,
            allSubcontractors = allSubcontractors
          )

        Option.when(issues.nonEmpty) {
          SubcontractorFinalValidationFailure(
            subcontractorId = subcontractor.subcontractorId,
            issues = issues,
            subbieResourceRef = subcontractor.subbieResourceRef
          )
        }
      }

    FinalValidationResult(failures = failures)
  }

  /** F1b - readiness validation.
    *
    * Runs only from EH03 "Accept and submit".
    *
    * Once the Mongo draft exists it is the authoritative Final Validation working state, so this method validates
    * draft.proposed directly and does not convert back to models.monthlyreturns.Subcontractor.
    *
    * With the failures-only draft design, draft.subcontractors contains only subcontractors which failed the original
    * F1.
    */
  def validateDraftSubcontractor(
    draft: FinalValidationDraft,
    subcontractorId: Long
  ): Try[Seq[FinalValidationDraftIssue]] =
    draft.subcontractor(subcontractorId) match {

      case Some(subcontractor) =>
        Try {
          draftValidationFields(
            subcontractor = subcontractor,
            allSubcontractors = draft.subcontractors
          ).distinct
            .map { field =>
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

  // ---------------------------------------------------------------------------
  // F1
  // ---------------------------------------------------------------------------

  private def initialValidationIssues(
    subcontractor: Subcontractor,
    allSubcontractors: Seq[Subcontractor]
  ): Seq[FinalValidationIssue] =
    initialValidationFields(
      subcontractor = subcontractor,
      allSubcontractors = allSubcontractors
    ).distinct
      .map { field =>
        FinalValidationIssue(
          field = field,
          value = field.valueFrom(subcontractor)
        )
      }

  private def initialValidationFields(
    subcontractor: Subcontractor,
    allSubcontractors: Seq[Subcontractor]
  ): Seq[FinalValidationField] =
    parseSubcontractorType(
      subcontractor.subcontractorType,
      subcontractor.subcontractorId
    ) match {

      case SoleTrader =>
        individualSubcontractorFinalValidation.validate(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validate(subcontractor)

      case Company =>
        companySubcontractorFinalValidation.validate(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validate(subcontractor)

      case Trust =>
        trustSubcontractorFinalValidation.validate(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validate(subcontractor)

      case Partnership =>
        partnershipSubcontractorFinalValidation.validate(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validate(subcontractor)
    }

  // ---------------------------------------------------------------------------
  // F1b
  // ---------------------------------------------------------------------------

  private def draftValidationFields(
    subcontractor: FinalValidationDraftSubcontractor,
    allSubcontractors: Seq[FinalValidationDraftSubcontractor]
  ): Seq[FinalValidationField] =
    parseSubcontractorType(subcontractor.subcontractorType, subcontractor.subcontractorId) match {

      case SoleTrader =>
        individualSubcontractorFinalValidation.validateDraft(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validateDraft(subcontractor)

      case Company =>
        companySubcontractorFinalValidation.validateDraft(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validateDraft(subcontractor)

      case Trust =>
        trustSubcontractorFinalValidation.validateDraft(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validateDraft(subcontractor)

      case Partnership =>
        partnershipSubcontractorFinalValidation.validateDraft(subcontractor, allSubcontractors) ++
          addressDetailsFinalValidation.validateDraft(subcontractor)
    }

  // ---------------------------------------------------------------------------
  // Shared helpers
  // ---------------------------------------------------------------------------

  private def parseSubcontractorType(
    subcontractorType: Option[String],
    subcontractorId: Long
  ): SubcontractorType =
    subcontractorType
      .flatMap { value =>
        Try(
          SubcontractorType.fromString(value)
        ).toOption
      }
      .getOrElse(
        throw new IllegalArgumentException(
          s"Unknown subcontractor type for subcontractor ID: $subcontractorId"
        )
      )

  /** Equivalent of FinalValidationField.valueFrom(Subcontractor), but for the authoritative draft proposed-value model.
    */
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
