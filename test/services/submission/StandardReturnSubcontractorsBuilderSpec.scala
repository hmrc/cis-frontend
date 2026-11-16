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

package services.submission

import models.UserAnswers
import models.monthlyreturns.{SelectedSubcontractor, Subcontractor}
import models.submission.SubcontractorType
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.TryValues
import org.scalatest.OptionValues.*
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.libs.json.Json

class StandardReturnSubcontractorsBuilderSpec extends AnyWordSpec with Matchers with TryValues {

  private def uaWithSelected(index: Int, s: SelectedSubcontractor): UserAnswers =
    UserAnswers("id", Json.obj())
      .set(SelectedSubcontractorPage(index), s)
      .success
      .value

  private def mkSub(
    id: Long,
    subType: Option[String],
    tradingName: Option[String] = None,
    firstName: Option[String] = Some("A"),
    secondName: Option[String] = Some("M"),
    surname: Option[String] = Some("B"),
    utr: Option[String] = Some("1234567890")
  ): Subcontractor =
    Subcontractor(
      subcontractorId = id,
      utr = utr,
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = firstName,
      nino = None,
      secondName = secondName,
      surname = surname,
      partnershipTradingName = None,
      tradingName = tradingName,
      subcontractorType = subType,
      addressLine1 = None,
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      country = None,
      postCode = None,
      emailAddress = None,
      phoneNumber = None,
      mobilePhoneNumber = None,
      worksReferenceNumber = None,
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

  "StandardReturnSubcontractorsBuilder.build" should {

    "throw when matching subcontractor has missing subcontractor type" in {
      val selected = SelectedSubcontractor(
        id = 1L,
        name = "A B",
        totalPaymentsMade = None,
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val ua = uaWithSelected(0, selected)

      val ex = intercept[RuntimeException] {
        StandardReturnSubcontractorsBuilder.build(
          ua,
          allSubs = Seq(mkSub(1L, None))
        )
      }

      ex.getMessage mustBe "Missing subcontractor type"
    }

    "throw when no subcontractors selected" in {
      val ua = UserAnswers("id", Json.obj())
      val ex = intercept[RuntimeException] {
        StandardReturnSubcontractorsBuilder.build(ua, allSubs = Seq.empty)
      }
      ex.getMessage must include("No subcontractors selected")
    }

    "build one ChrisStandardSubcontractor from selected + matching BE subcontractor (soletrader, blank tradingName => person name)" in {
      val selected = SelectedSubcontractor(
        id = 1L,
        name = "A B",
        totalPaymentsMade = Some(BigDecimal(100)),
        costOfMaterials = Some(BigDecimal(10)),
        totalTaxDeducted = Some(BigDecimal(20))
      )

      val ua = uaWithSelected(0, selected)

      val allSubs = Seq(
        mkSub(
          id = 1L,
          subType = Some("soletrader"),
          tradingName = Some("   ")
        )
      )

      val out = StandardReturnSubcontractorsBuilder.build(ua, allSubs)

      out.length mustBe 1
      val s = out.head
      s.subcontractorType mustBe SubcontractorType.SoleTrader
      s.name.value.first mustBe "A"
      s.name.value.middle mustBe Some("M")
      s.name.value.last mustBe "B"
      s.utr mustBe Some("1234567890")
      s.totalPayments mustBe Some(BigDecimal(100))
      s.costOfMaterials mustBe Some(BigDecimal(10))
      s.totalDeducted mustBe Some(BigDecimal(20))
    }

    "throw when selected subcontractor id is not in BE list" in {
      val selected = SelectedSubcontractor(
        id = 999L,
        name = "Missing",
        totalPaymentsMade = None,
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val ua = uaWithSelected(0, selected)

      val ex = intercept[RuntimeException] {
        StandardReturnSubcontractorsBuilder.build(ua, allSubs = Seq(mkSub(1L, Some("soletrader"))))
      }

      ex.getMessage must include("No subcontractor found with id 999")
    }
  }

  "StandardReturnSubcontractorsBuilder.build with id" should {

    "build subcontractors using ID-scoped answers" in {
      val id = "test-id"

      val selected = SelectedSubcontractor(
        id = 1L,
        name = "A B",
        totalPaymentsMade = Some(BigDecimal(100)),
        costOfMaterials = Some(BigDecimal(10)),
        totalTaxDeducted = Some(BigDecimal(20))
      )

      val ua = UserAnswers("id", Json.obj())
        .setOrException(SelectedSubcontractorPage.all(id), Map(0 -> selected))

      val allSubs = Seq(mkSub(1L, Some("soletrader")))

      val out = StandardReturnSubcontractorsBuilder.build(ua, id, allSubs)

      out.length mustBe 1
      out.head.subcontractorType mustBe SubcontractorType.SoleTrader
      out.head.totalPayments mustBe Some(BigDecimal(100))
      out.head.costOfMaterials mustBe Some(BigDecimal(10))
      out.head.totalDeducted mustBe Some(BigDecimal(20))
    }

    "throw when ID-scoped subcontractors are missing" in {
      val id = "test-id"

      val selected = SelectedSubcontractor(
        id = 1L,
        name = "A B",
        totalPaymentsMade = Some(BigDecimal(100)),
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val ua = uaWithSelected(0, selected)

      val ex = intercept[RuntimeException] {
        StandardReturnSubcontractorsBuilder.build(ua, id, Seq(mkSub(1L, Some("soletrader"))))
      }

      ex.getMessage must include("No subcontractors selected")
    }

    "use subcontractors from the correct ID-scoped journey" in {
      val firstSelected = SelectedSubcontractor(
        id = 1L,
        name = "First",
        totalPaymentsMade = Some(BigDecimal(100)),
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val secondSelected = SelectedSubcontractor(
        id = 2L,
        name = "Second",
        totalPaymentsMade = Some(BigDecimal(200)),
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val ua = UserAnswers("id", Json.obj())
        .setOrException(SelectedSubcontractorPage.all("first-id"), Map(0 -> firstSelected))
        .setOrException(SelectedSubcontractorPage.all("second-id"), Map(0 -> secondSelected))

      val allSubs = Seq(
        mkSub(1L, Some("soletrader")),
        mkSub(2L, Some("soletrader"))
      )

      val firstResult  = StandardReturnSubcontractorsBuilder.build(ua, "first-id", allSubs)
      val secondResult = StandardReturnSubcontractorsBuilder.build(ua, "second-id", allSubs)

      firstResult.length mustBe 1
      firstResult.head.totalPayments mustBe Some(BigDecimal(100))

      secondResult.length mustBe 1
      secondResult.head.totalPayments mustBe Some(BigDecimal(200))
    }
  }

  "StandardReturnSubcontractorsBuilder.build with selected subcontractors" should {

    "build subcontractors directly from the selected sequence" in {
      val selected = SelectedSubcontractor(
        id = 1L,
        name = "A B",
        totalPaymentsMade = Some(BigDecimal(100)),
        costOfMaterials = Some(BigDecimal(10)),
        totalTaxDeducted = Some(BigDecimal(20))
      )

      val allSubs = Seq(mkSub(1L, Some("soletrader")))

      val out = StandardReturnSubcontractorsBuilder.build(Seq(selected), allSubs)

      out.length mustBe 1
      out.head.subcontractorType mustBe SubcontractorType.SoleTrader
      out.head.totalPayments mustBe Some(BigDecimal(100))
      out.head.costOfMaterials mustBe Some(BigDecimal(10))
      out.head.totalDeducted mustBe Some(BigDecimal(20))
    }
  }
}
