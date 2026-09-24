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
import play.api.i18n.Messages

class UpdateSubcontractorDetailsPageModelBuilderSpec extends SpecBase {

  private val builder =
    new UpdateSubcontractorDetailsPageModelBuilder()

  private val changeUrl =
    (field: FinalValidationField, target: FinalValidationChangeTarget) => s"/change/${field.key}/${target.key}"

  private def subcontractor(
    proposed: FinalValidationSubcontractorDetails,
    issues: Seq[FinalValidationDraftIssue],
    subcontractorType: String = "soletrader"
  ): FinalValidationDraftSubcontractor =
    FinalValidationDraftSubcontractor(
      subcontractorId = 1L,
      subbieResourceRef = 2L,
      baseVersion = Some(1),
      subcontractorType = Some(subcontractorType),
      displayName = "Test Subcontractor",
      base = proposed,
      proposed = proposed,
      changedTargets = Set.empty,
      issues = issues,
      readiness = FinalValidationReadiness.Incomplete
    )

  "UpdateSubcontractorDetailsPageModelBuilder" - {

    "must build a sole trader name row when a name field has an issue" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          firstName = Some("John"),
          secondName = Some("Paul"),
          surname = Some("Smith")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.FirstName.key,
                value = Some("John")
              )
            )
          ),
          changeUrl
        )

      result must contain(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.FirstName,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.subcontractorName",
          value = Some("John Paul Smith"),
          changeUrl =
            s"/change/${FinalValidationField.FirstName.key}/${FinalValidationChangeTarget.SubcontractorName.key}"
        )
      )
    }

    "must build a trading name row when trading name has an issue" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          tradingName = Some("Smith Trading")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.TradingName.key,
                value = Some("Smith Trading")
              )
            )
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.TradingName,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.names",
          value = Some(
            summon[Messages](
              "finalvalidations.updateSubcontractorDetails.soleTrader.tradingName"
            )
          ),
          changeUrl = s"/change/${FinalValidationField.TradingName.key}/${FinalValidationChangeTarget.Names.key}"
        ),
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.TradingName,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.tradingName",
          value = Some("Smith Trading"),
          changeUrl = s"/change/${FinalValidationField.TradingName.key}/${FinalValidationChangeTarget.TradingName.key}"
        )
      )
    }

    "must build YesNo and value rows when an optional value is present" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          utr = Some("1234567890")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.Utr.key,
                value = Some("1234567890")
              )
            )
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.Utr,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addUtr",
          value = Some(summon[Messages]("site.yes")),
          changeUrl = s"/change/${FinalValidationField.Utr.key}/${FinalValidationChangeTarget.UtrYesNo.key}"
        ),
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.Utr,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.utr",
          value = Some("1234567890"),
          changeUrl = s"/change/${FinalValidationField.Utr.key}/${FinalValidationChangeTarget.Utr.key}"
        )
      )
    }

    "must only build the YesNo row when an optional value is absent" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          utr = None
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.Utr.key,
                value = None
              )
            )
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.Utr,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addUtr",
          value = Some(summon[Messages]("site.no")),
          changeUrl = s"/change/${FinalValidationField.Utr.key}/${FinalValidationChangeTarget.UtrYesNo.key}"
        )
      )
    }

    "must combine address fields into an address row" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = Some("Test Area"),
          addressLine3 = Some("Test Town"),
          postcode = Some("AA1 1AA"),
          country = Some("United Kingdom")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.AddressLine1.key,
                value = Some("1 Test Street")
              )
            )
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.AddressLine1,
          labelKey = "finalvalidations.updateSubcontractorDetails.soleTrader.addAddress",
          value = Some(summon[Messages]("site.yes")),
          changeUrl =
            s"/change/${FinalValidationField.AddressLine1.key}/${FinalValidationChangeTarget.AddressYesNo.key}"
        ),
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.AddressLine1,
          labelKey = "finalvalidations.updateSubcontractorDetails.address",
          value = Some(
            """1 Test Street
              |Test Area
              |Test Town
              |AA1 1AA
              |United Kingdom""".stripMargin
          ),
          changeUrl = s"/change/${FinalValidationField.AddressLine1.key}/${FinalValidationChangeTarget.Address.key}"
        )
      )
    }

    "must build contact details rows for failed contact fields" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          emailAddress = Some("test@example.com"),
          phoneNumber = Some("01234567890")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.EmailAddress.key,
                value = Some("test@example.com")
              ),
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.PhoneNumber.key,
                value = Some("01234567890")
              )
            )
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.EmailAddress,
          labelKey = "finalvalidations.updateSubcontractorDetails.addContactDetails",
          value = Some(summon[Messages]("site.yes")),
          changeUrl =
            s"/change/${FinalValidationField.EmailAddress.key}/${FinalValidationChangeTarget.ContactDetailsYesNo.key}"
        ),
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.EmailAddress,
          labelKey = "finalvalidations.updateSubcontractorDetails.emailAddress",
          value = Some("test@example.com"),
          changeUrl =
            s"/change/${FinalValidationField.EmailAddress.key}/${FinalValidationChangeTarget.EmailAddress.key}"
        ),
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.PhoneNumber,
          labelKey = "finalvalidations.updateSubcontractorDetails.phoneNumber",
          value = Some("01234567890"),
          changeUrl = s"/change/${FinalValidationField.PhoneNumber.key}/${FinalValidationChangeTarget.PhoneNumber.key}"
        )
      )
    }

    "must build shared works reference number rows" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          worksReferenceNumber = Some("WRN123")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.WorksReferenceNumber.key,
                value = Some("WRN123")
              )
            )
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.WorksReferenceNumber,
          labelKey = "finalvalidations.updateSubcontractorDetails.addWorksReferenceNumber",
          value = Some(summon[Messages]("site.yes")),
          changeUrl =
            s"/change/${FinalValidationField.WorksReferenceNumber.key}/${FinalValidationChangeTarget.WorksReferenceNumberYesNo.key}"
        ),
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.WorksReferenceNumber,
          labelKey = "finalvalidations.updateSubcontractorDetails.worksReferenceNumber",
          value = Some("WRN123"),
          changeUrl =
            s"/change/${FinalValidationField.WorksReferenceNumber.key}/${FinalValidationChangeTarget.WorksReferenceNumber.key}"
        )
      )
    }

    "must use no name provided when a company name is missing" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          tradingName = None
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.TradingName.key,
                value = None
              )
            ),
            subcontractorType = "company"
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.TradingName,
          labelKey = "finalvalidations.updateSubcontractorDetails.company.name",
          value = Some(
            summon[Messages](
              "finalvalidations.updateSubcontractorDetails.noNameProvided"
            )
          ),
          changeUrl = s"/change/${FinalValidationField.TradingName.key}/${FinalValidationChangeTarget.TradingName.key}"
        )
      )
    }

    "must use no name provided when a trust name is empty" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          tradingName = Some("")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.TradingName.key,
                value = Some("")
              )
            ),
            subcontractorType = "trust"
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.TradingName,
          labelKey = "finalvalidations.updateSubcontractorDetails.trust.name",
          value = Some(
            summon[Messages](
              "finalvalidations.updateSubcontractorDetails.noNameProvided"
            )
          ),
          changeUrl = s"/change/${FinalValidationField.TradingName.key}/${FinalValidationChangeTarget.TradingName.key}"
        )
      )
    }

    "must use no name provided when a partnership name is blank" in {

      given Messages = messages(app)

      val details =
        FinalValidationSubcontractorDetails(
          partnershipTradingName = Some("   ")
        )

      val result =
        builder.build(
          subcontractor(
            proposed = details,
            issues = Seq(
              FinalValidationDraftIssue(
                fieldKey = FinalValidationField.PartnershipTradingName.key,
                value = Some("   ")
              )
            ),
            subcontractorType = "partnership"
          ),
          changeUrl
        )

      result mustBe Seq(
        UpdateSubcontractorDetailsRow(
          field = FinalValidationField.PartnershipTradingName,
          labelKey = "finalvalidations.updateSubcontractorDetails.partnership.name",
          value = Some(
            summon[Messages](
              "finalvalidations.updateSubcontractorDetails.noNameProvided"
            )
          ),
          changeUrl =
            s"/change/${FinalValidationField.PartnershipTradingName.key}/${FinalValidationChangeTarget.PartnershipTradingName.key}"
        )
      )
    }

    "must return no rows when there are no issues" in {

      given Messages = messages(app)

      val result =
        builder.build(
          subcontractor(
            proposed = FinalValidationSubcontractorDetails(),
            issues = Seq.empty
          ),
          changeUrl
        )

      result mustBe Seq.empty
    }

    "must throw when an issue contains an unknown field key" in {

      given Messages = messages(app)

      val subbie =
        subcontractor(
          proposed = FinalValidationSubcontractorDetails(),
          issues = Seq(
            FinalValidationDraftIssue(
              fieldKey = "unknown",
              value = None
            )
          )
        )

      val exception =
        intercept[IllegalArgumentException] {
          builder.build(subbie, changeUrl)
        }

      exception.getMessage mustBe
        "Unknown Final Validation field key: unknown"
    }

    "must use the proposed partnership name as the display name" in {

      given Messages = messages(app)

      val subbie =
        subcontractor(
          proposed = FinalValidationSubcontractorDetails(
            partnershipTradingName = Some("Alice")
          ),
          issues = Seq.empty,
          subcontractorType = "partnership"
        ).copy(
          displayName = "Alice|"
        )

      builder.displayName(subbie) mustBe "Alice"
    }

    "must use no name provided as the display name when the proposed name is missing" in {

      given Messages = messages(app)

      val subbie =
        subcontractor(
          proposed = FinalValidationSubcontractorDetails(
            tradingName = None
          ),
          issues = Seq.empty,
          subcontractorType = "company"
        ).copy(
          displayName = "Old Company Name"
        )

      builder.displayName(subbie) mustBe
        summon[Messages](
          "finalvalidations.updateSubcontractorDetails.noNameProvided"
        )
    }
  }
}
