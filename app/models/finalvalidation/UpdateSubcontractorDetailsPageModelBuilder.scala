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
import models.monthlyreturns.Subcontractor
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
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] = {

    val subcontractorType =
      subcontractor.subcontractorType
        .flatMap(value => Try(SubcontractorType.fromString(value)).toOption)

    val rows =
      subcontractorType match {
        case Some(SoleTrader)  => soleTraderRows(subcontractor, failure, changeUrl)
        case Some(Company)     => companyRows(subcontractor, failure, changeUrl)
        case Some(Trust)       => trustRows(subcontractor, failure, changeUrl)
        case Some(Partnership) => partnershipRows(subcontractor, failure, changeUrl)
        case _                 => Seq.empty
      }

    rows ++ sharedRows(subcontractor, failure, changeUrl)
  }

  private def soleTraderRows(
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      groupedRow(
        failure = failure,
        fields = soleTraderNameFields,
        labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.subcontractorName",
        value = soleTraderName(subcontractor),
        target = FinalValidationChangeTarget.SubcontractorName,
        changeUrl = changeUrl
      ),
      valueRow(
        failure = failure,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.tradingName",
        value = subcontractor.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Utr,
        value = subcontractor.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Nino,
        value = subcontractor.nino,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addNino",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.nino",
        yesNoTarget = FinalValidationChangeTarget.NinoYesNo,
        valueTarget = FinalValidationChangeTarget.Nino,
        changeUrl = changeUrl
      )
    ).flatten

  private def companyRows(
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      valueRow(
        failure = failure,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.company.name",
        value = subcontractor.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Utr,
        value = subcontractor.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.company.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.company.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Crn,
        value = subcontractor.crn,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.company.addCrn",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.company.crn",
        yesNoTarget = FinalValidationChangeTarget.CrnYesNo,
        valueTarget = FinalValidationChangeTarget.Crn,
        changeUrl = changeUrl
      )
    ).flatten

  private def trustRows(
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      valueRow(
        failure = failure,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.trust.name",
        value = subcontractor.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Utr,
        value = subcontractor.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.trust.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.trust.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      )
    ).flatten

  private def partnershipRows(
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    Seq(
      valueRow(
        failure = failure,
        field = PartnershipTradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.partnership.name",
        value = subcontractor.partnershipTradingName,
        target = FinalValidationChangeTarget.PartnershipTradingName,
        changeUrl = changeUrl
      ),
      valueRow(
        failure = failure,
        field = TradingName,
        labelKey = "finalvalidations.updateSubcontractorDetails.partnership.nominatedPartner",
        value = subcontractor.tradingName,
        target = FinalValidationChangeTarget.TradingName,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Utr,
        value = subcontractor.utr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.utr",
        yesNoTarget = FinalValidationChangeTarget.UtrYesNo,
        valueTarget = FinalValidationChangeTarget.Utr,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = PartnerUtr,
        value = subcontractor.partnerUtr,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addPartnerUtr",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.partnerUtr",
        yesNoTarget = FinalValidationChangeTarget.PartnerUtrYesNo,
        valueTarget = FinalValidationChangeTarget.PartnerUtr,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Nino,
        value = subcontractor.nino,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addPartnerNino",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.partnerNino",
        yesNoTarget = FinalValidationChangeTarget.NinoYesNo,
        valueTarget = FinalValidationChangeTarget.Nino,
        changeUrl = changeUrl
      ),
      optionalRows(
        failure = failure,
        field = Crn,
        value = subcontractor.crn,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.addPartnerCrn",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.partnership.partnerCrn",
        yesNoTarget = FinalValidationChangeTarget.CrnYesNo,
        valueTarget = FinalValidationChangeTarget.Crn,
        changeUrl = changeUrl
      )
    ).flatten

  private def sharedRows(
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    addressRows(subcontractor, failure, changeUrl) ++
      contactRows(subcontractor, failure, changeUrl) ++
      optionalRows(
        failure = failure,
        field = WorkReferenceNumber,
        value = subcontractor.worksReferenceNumber,
        yesNoLabelKey = "finalvalidations.updateSubcontractorDetails.addWorksReferenceNumber",
        valueLabelKey = "finalvalidations.updateSubcontractorDetails.worksReferenceNumber",
        yesNoTarget = FinalValidationChangeTarget.WorksReferenceNumberYesNo,
        valueTarget = FinalValidationChangeTarget.WorksReferenceNumber,
        changeUrl = changeUrl
      )

  private def addressRows(
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    firstFailure(failure, addressFields).toSeq.flatMap { failedField =>

      val value = combined(
        subcontractor.addressLine1,
        subcontractor.addressLine2,
        subcontractor.addressLine3,
        subcontractor.addressLine4,
        subcontractor.postCode,
        subcontractor.country
      )

      val typeKey =
        subcontractorType(subcontractor) match {
          case SoleTrader  => "soleTrader"
          case Company     => "company"
          case Trust       => "trust"
          case Partnership => "partnership"
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
    subcontractor: Subcontractor,
    failure: SubcontractorFinalValidationFailure,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] = {

    val failedFields =
      failure.issues
        .map(_.field)
        .filter(contactFields.contains)
        .distinct

    failedFields.headOption.toSeq.flatMap { controllingField =>

      val hasContacts =
        Seq(
          subcontractor.emailAddress,
          subcontractor.phoneNumber,
          subcontractor.mobilePhoneNumber
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
        yesNoRow +: failedFields.flatMap {
          case EmailAddress      =>
            presentRow(
              EmailAddress,
              "finalvalidations.updateSubcontractorDetails.emailAddress",
              subcontractor.emailAddress,
              FinalValidationChangeTarget.EmailAddress,
              changeUrl
            )
          case PhoneNumber       =>
            presentRow(
              PhoneNumber,
              "finalvalidations.updateSubcontractorDetails.phoneNumber",
              subcontractor.phoneNumber,
              FinalValidationChangeTarget.PhoneNumber,
              changeUrl
            )
          case MobilePhoneNumber =>
            presentRow(
              MobilePhoneNumber,
              "finalvalidations.updateSubcontractorDetails.mobilePhoneNumber",
              subcontractor.mobilePhoneNumber,
              FinalValidationChangeTarget.MobilePhoneNumber,
              changeUrl
            )
          case _                 => Seq.empty
        }
      }
    }
  }

  private def optionalRows(
    failure: SubcontractorFinalValidationFailure,
    field: FinalValidationField,
    value: Option[String],
    yesNoLabelKey: String,
    valueLabelKey: String,
    yesNoTarget: FinalValidationChangeTarget,
    valueTarget: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  )(implicit messages: Messages): Seq[UpdateSubcontractorDetailsRow] =
    if (hasFailure(failure, field)) {
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
    failure: SubcontractorFinalValidationFailure,
    field: FinalValidationField,
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): Seq[UpdateSubcontractorDetailsRow] =
    if (hasFailure(failure, field)) {
      Seq(row(field, labelKey, value, target, changeUrl))
    } else {
      Seq.empty
    }

  private def groupedRow(
    failure: SubcontractorFinalValidationFailure,
    fields: Set[FinalValidationField],
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): Seq[UpdateSubcontractorDetailsRow] =
    firstFailure(failure, fields).toSeq.map { failedField =>
      row(failedField, labelKey, value, target, changeUrl)
    }

  private def presentRow(
    field: FinalValidationField,
    labelKey: String,
    value: Option[String],
    target: FinalValidationChangeTarget,
    changeUrl: ChangeUrl
  ): Seq[UpdateSubcontractorDetailsRow] =
    if (present(value)) {
      Seq(row(field, labelKey, value, target, changeUrl))
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
      changeUrl = changeUrl(field, target)
    )

  private def hasFailure(
    failure: SubcontractorFinalValidationFailure,
    field: FinalValidationField
  ): Boolean =
    failure.issues.exists(_.field == field)

  private def firstFailure(
    failure: SubcontractorFinalValidationFailure,
    fields: Set[FinalValidationField]
  ): Option[FinalValidationField] =
    failure.issues.map(_.field).find(fields.contains)

  private def present(value: Option[String]): Boolean =
    value.exists(_.trim.nonEmpty)

  private def combined(values: Option[String]*): Option[String] =
    val result = values.flatten.map(_.trim).filter(_.nonEmpty).mkString(" ")
    Option.when(result.nonEmpty)(result)

  private def yesNo(value: Boolean)(implicit messages: Messages): String =
    messages(if (value) "site.yes" else "site.no")

  private def subcontractorType(subcontractor: Subcontractor): SubcontractorType =
    subcontractor.subcontractorType
      .flatMap(value => Try(SubcontractorType.fromString(value)).toOption)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Unknown Subcontractor type for subcontractorId: ${subcontractor.subcontractorId}"
        )
      )

  private def soleTraderName(subcontractor: Subcontractor): Option[String] = {
    val result =
      Seq(
        subcontractor.firstName,
        subcontractor.secondName,
        subcontractor.surname
      ).flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .mkString(" ")

    Option.when(result.nonEmpty)(result)
  }

}
