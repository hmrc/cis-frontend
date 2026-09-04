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

import models.finalvalidation.FinalValidationField.*
import models.submission.SubcontractorType
import models.submission.SubcontractorType.*
import play.api.i18n.Messages

import scala.util.Try
import javax.inject.{Inject, Singleton}

@Singleton
class UpdateSubcontractorDetailsPageModelBuilder @Inject() {

  private type ChangeUrl =
    (FinalValidationField, FinalValidationChangeTarget) => String

  private val soleTraderNameFields =
    Set[FinalValidationField](
      FirstName,
      SecondName,
      Surname
    )

  private val addressFields =
    Set[FinalValidationField](
      AddressLine1,
      AddressLine2,
      AddressLine3,
      AddressLine4,
      PostCode,
      Country
    )

  private val contactFields =
    Set[FinalValidationField](
      EmailAddress,
      PhoneNumber,
      MobilePhoneNumber
    )

  def build(
    subcontractor: FinalValidationDraftSubcontractor,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] = {

    val details = subcontractor.proposed

    val rows =
      subcontractorType(subcontractor) match {

        case SoleTrader =>
          soleTraderRows(
            subcontractor,
            details,
            changeUrl
          )

        case Company =>
          companyRows(
            subcontractor,
            details,
            changeUrl
          )

        case Trust =>
          trustRows(
            subcontractor,
            details,
            changeUrl
          )

        case Partnership =>
          partnershipRows(
            subcontractor,
            details,
            changeUrl
          )
      }

    rows ++
      sharedRows(
        subcontractor,
        details,
        changeUrl
      )
  }

  private def soleTraderRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      groupedRow(
        subcontractor = subcontractor,
        fields = soleTraderNameFields,
        labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.subcontractorName",
        value = soleTraderName(details),
        target = FinalValidationChangeTarget.SubcontractorName,
        changeUrl = changeUrl
      ),
      valueRow(
        subcontractor = subcontractor,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.tradingName",
        value = details.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Utr,
        value = details.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Nino,
        value = details.nino,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addNino",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.nino",
        yesNoTarget = FinalValidationChangeTarget.NinoYesNo,
        valueTarget = FinalValidationChangeTarget.Nino,
        changeUrl = changeUrl
      )
    ).flatten

  private def companyRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      valueRow(
        subcontractor = subcontractor,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.company.name",
        value = details.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Utr,
        value = details.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.company.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.company.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Crn,
        value = details.crn,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.company.addCrn",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.company.crn",
        yesNoTarget = FinalValidationChangeTarget.CrnYesNo,
        valueTarget = FinalValidationChangeTarget.Crn,
        changeUrl = changeUrl
      )
    ).flatten

  private def trustRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      valueRow(
        subcontractor = subcontractor,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.trust.name",
        value = details.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Utr,
        value = details.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.trust.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.trust.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      )
    ).flatten

  private def partnershipRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      valueRow(
        subcontractor = subcontractor,
        field = PartnershipTradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.partnership.name",
        value = details.partnershipTradingName,
        target = FinalValidationChangeTarget.PartnershipTradingName,
        changeUrl = changeUrl
      ),
      valueRow(
        subcontractor = subcontractor,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.partnership.nominatedPartner",
        value = details.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Utr,
        value = details.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = PartnerUtr,
        value = details.partnerUtr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addPartnerUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.partnerUtr",
        yesNoTarget = FinalValidationChangeTarget.PartnerUtrYesNo,
        valueTarget = FinalValidationChangeTarget.PartnerUtr,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Nino,
        value = details.nino,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addPartnerNino",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.partnerNino",
        yesNoTarget = FinalValidationChangeTarget.NinoYesNo,
        valueTarget = FinalValidationChangeTarget.Nino,
        changeUrl = changeUrl
      ),
      optionalRows(
        subcontractor = subcontractor,
        field = Crn,
        value = details.crn,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addPartnerCrn",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.partnerCrn",
        yesNoTarget = FinalValidationChangeTarget.CrnYesNo,
        valueTarget = FinalValidationChangeTarget.Crn,
        changeUrl = changeUrl
      )
    ).flatten

  private def sharedRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    addressRows(
      subcontractor,
      details,
      changeUrl
    ) ++
      contactRows(
        subcontractor,
        details,
        changeUrl
      ) ++
      optionalRows(
        subcontractor = subcontractor,
        field = WorkReferenceNumber,
        value = details.worksReferenceNumber,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.addWorksReferenceNumber",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.worksReferenceNumber",
        yesNoTarget = FinalValidationChangeTarget.WorksReferenceNumberYesNo,
        valueTarget = FinalValidationChangeTarget.WorksReferenceNumber,
        changeUrl = changeUrl
      )

  private def addressRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    firstIssue(
      subcontractor,
      addressFields
    ).toSeq.flatMap { failedField =>

      val value =
        combined(
          details.addressLine1,
          details.addressLine2,
          details.addressLine3,
          details.addressLine4,
          details.postcode,
          details.country
        )

      val typeKey =
        subcontractorType(subcontractor) match {
          case SoleTrader =>
            "soleTrader"

          case Company =>
            "company"

          case Trust =>
            "trust"

          case Partnership =>
            "partnership"
        }

      yesNoAndValueRows(
        failedField = failedField,
        value = value,
        yesNoLabelKey = s"finalvalidations.updateSubcontractorDetails.$typeKey.addAddress",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.address",
        yesNoTarget = FinalValidationChangeTarget.AddressYesNo,
        valueTarget = FinalValidationChangeTarget.Address,
        changeUrl = changeUrl
      )
    }

  private def contactRows(
    subcontractor: FinalValidationDraftSubcontractor,
    details: FinalValidationSubcontractorDetails,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] = {

    val failedFields =
      issueFields(subcontractor)
        .filter(contactFields.contains)
        .distinct

    failedFields.headOption.toSeq.flatMap { controllingField =>

      val hasContacts =
        Seq(
          details.emailAddress,
          details.phoneNumber,
          details.mobilePhoneNumber
        ).exists(present)

      val yesNoRow =
        row(
          field = controllingField,
          labelKey = "finalvalidations.updateSubcontractorDetails.addContactDetails",
          value = Some(yesNo(hasContacts)),
          target = FinalValidationChangeTarget.ContactDetailsYesNo,
          changeUrl = changeUrl
        )

      if (!hasContacts) {
        Seq(yesNoRow)
      } else {
        yesNoRow +:
          failedFields.flatMap {

            case EmailAddress =>
              presentRow(
                EmailAddress,
                "finalvalidations.updateSubcontractorDetails.emailAddress",
                details.emailAddress,
                FinalValidationChangeTarget.EmailAddress,
                changeUrl
              )

            case PhoneNumber =>
              presentRow(
                PhoneNumber,
                "finalvalidations.updateSubcontractorDetails.phoneNumber",
                details.phoneNumber,
                FinalValidationChangeTarget.PhoneNumber,
                changeUrl
              )

            case MobilePhoneNumber =>
              presentRow(
                MobilePhoneNumber,
                "finalvalidations.updateSubcontractorDetails.mobilePhoneNumber",
                details.mobilePhoneNumber,
                FinalValidationChangeTarget.MobilePhoneNumber,
                changeUrl
              )

            case _ =>
              Seq.empty
          }
      }
    }
  }

  private def optionalRows(
    subcontractor: FinalValidationDraftSubcontractor,
    field: FinalValidationField,
    value: Option[String],
    yesNoLabelKey: String,
    valueLabelKey: String,
    yesNoTarget: FinalValidationChangeTarget,
    valueTarget: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    if (hasIssue(subcontractor, field)) {
      yesNoAndValueRows(
        failedField = field,
        value = value,
        yesNoLabelKey = yesNoLabelKey,
        valueLabelKey = valueLabelKey,
        yesNoTarget = yesNoTarget,
        valueTarget = valueTarget,
        changeUrl = changeUrl
      )
    } else {
      Seq.empty
    }

  private def yesNoAndValueRows(
    failedField: FinalValidationField,
    value: Option[String],
    yesNoLabelKey: String,
    valueLabelKey: String,
    yesNoTarget: FinalValidationChangeTarget,
    valueTarget: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] = {

    val supplied = present(value)

    val yesNoRow =
      row(
        field = failedField,
        labelKey = yesNoLabelKey,
        value = Some(yesNo(supplied)),
        target = yesNoTarget,
        changeUrl = changeUrl
      )

    if (supplied) {
      Seq(
        yesNoRow,
        row(
          field = failedField,
          labelKey = valueLabelKey,
          value = value,
          target = valueTarget,
          changeUrl = changeUrl
        )
      )
    } else {
      Seq(yesNoRow)
    }
  }

  private def valueRow(
    subcontractor: FinalValidationDraftSubcontractor,
    field: FinalValidationField,
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): Seq[UpdateSubcontractorDetailsRow] =
    if (hasIssue(subcontractor, field)) {
      Seq(
        row(
          field,
          labelKey,
          value,
          target,
          changeUrl
        )
      )
    } else {
      Seq.empty
    }

  private def groupedRow(
    subcontractor: FinalValidationDraftSubcontractor,
    fields: Set[FinalValidationField],
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): Seq[UpdateSubcontractorDetailsRow] =
    firstIssue(
      subcontractor,
      fields
    ).toSeq.map { failedField =>
      row(
        failedField,
        labelKey,
        value,
        target,
        changeUrl
      )
    }

  private def presentRow(
    field: FinalValidationField,
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): Seq[UpdateSubcontractorDetailsRow] =
    if (present(value)) {
      Seq(
        row(
          field,
          labelKey,
          value,
          target,
          changeUrl
        )
      )
    } else {
      Seq.empty
    }

  private def row(
    field: FinalValidationField,
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): UpdateSubcontractorDetailsRow =
    UpdateSubcontractorDetailsRow(
      field = field,
      labelKey = labelKey,
      value = value,
      changeUrl = changeUrl(
        field,
        target
      )
    )

  private def hasIssue(
    subcontractor: FinalValidationDraftSubcontractor,
    field: FinalValidationField
  ): Boolean =
    issueFields(subcontractor)
      .contains(field)

  private def firstIssue(
    subcontractor: FinalValidationDraftSubcontractor,
    fields: Set[FinalValidationField]
  ): Option[FinalValidationField] =
    issueFields(subcontractor)
      .find(fields.contains)

  private def issueFields(
    subcontractor: FinalValidationDraftSubcontractor
  ): Seq[FinalValidationField] =
    subcontractor.issues.map { issue =>
      FinalValidationField
        .fromKey(issue.fieldKey)
        .getOrElse(
          throw new IllegalArgumentException(
            s"Unknown Final Validation field key: ${issue.fieldKey}"
          )
        )
    }

  private def present(
    value: Option[String]
  ): Boolean =
    value.exists(
      _.trim.nonEmpty
    )

  private def combined(
    values: Option[String]*
  ): Option[String] = {

    val result =
      values.flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .mkString(" ")

    Option.when(
      result.nonEmpty
    )(result)
  }

  private def yesNo(
    value: Boolean
  )(implicit messages: Messages): String =
    messages(
      if (value) {
        "site.yes"
      } else {
        "site.no"
      }
    )

  private def subcontractorType(
    subcontractor: FinalValidationDraftSubcontractor
  ): SubcontractorType =
    subcontractor.subcontractorType
      .flatMap { value =>
        Try(
          SubcontractorType.fromString(value)
        ).toOption
      }
      .getOrElse(
        throw new IllegalArgumentException(
          s"Unknown Subcontractor type for subcontractorId: " +
            subcontractor.subcontractorId
        )
      )

  private def soleTraderName(
    details: FinalValidationSubcontractorDetails
  ): Option[String] = {

    val result =
      Seq(
        details.firstName,
        details.secondName,
        details.surname
      ).flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .mkString(" ")

    Option.when(
      result.nonEmpty
    )(result)
  }
}
