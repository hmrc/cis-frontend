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

package services

import base.SpecBase
import models.UserAnswers
import models.requests.GetMonthlyReturnForEditRequest
import models.monthlyreturns.*
import models.submission.SubcontractorType
import pages.amend.WhichSubcontractorsToAddPage
import play.api.libs.json.Json
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.Mockito.*

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class SubcontractorServiceSpec extends SpecBase {

  given hc: HeaderCarrier = HeaderCarrier()
  given ExecutionContext  = ExecutionContext.global

  private val monthlyReturnService = mock(classOf[MonthlyReturnService])
  private val service              = new SubcontractorService(monthlyReturnService)

  "SubcontractorService.verificationPeriodStart" - {

    "returns 6 April of the tax year two years ago (date on/after 6 April)" in {
      // today 2026-01-29 -> two years ago 2024-01-29 which is before 6 April 2024 => taxYear = 2023
      service.verificationPeriodStart(LocalDate.of(2026, 1, 29)) mustBe LocalDate.of(2023, 4, 6)
    }

    "returns 6 April of the tax year two years ago (date after 6 April)" in {
      // today 2026-12-01 -> two years ago 2024-12-01 which is after 6 April 2024 => taxYear = 2024
      service.verificationPeriodStart(LocalDate.of(2026, 12, 1)) mustBe LocalDate.of(2024, 4, 6)
    }
  }

  "SubcontractorService.verificationRequired" - {

    "returns true when not verified" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("N"),
        verificationDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        verificationPeriodStart = start
      ) mustBe true
    }

    "returns true when verified but verificationDate missing" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = None,
        lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        verificationPeriodStart = start
      ) mustBe true
    }

    "returns false when verificationDate is within period even if lastMonthlyReturnDate is before period" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2024, 4, 6, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 4, 5, 0, 0)),
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns false when verificationDate is within period and lastMonthlyReturnDate is missing" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2024, 6, 1, 0, 0)),
        lastMonthlyReturnDate = None,
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns false when lastMonthlyReturnDate is within period" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2020, 1, 1, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 5, 1, 0, 0)),
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns false when lastMonthlyReturnDate is exactly on the period start" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2020, 1, 1, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 4, 6, 0, 0)),
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns true when both verificationDate and lastMonthlyReturnDate are before period" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2024, 4, 5, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 4, 5, 0, 0)),
        verificationPeriodStart = start
      ) mustBe true
    }
  }

  "SubcontractorService.buildSelectSubcontractorPage" - {

    "preselects last-month subcontractors and sets verificationRequired Yes/No" in {
      val response = mkResponse(
        items = Seq(mkItem(1001L)),
        subs = Seq(
          mkSubcontractor(
            id = 1L,
            ref = Some(1001L),
            verified = Some("N"),
            verificationDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
            lastMrDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
            verificationNumber = Some("123456"),
            taxTreatment = Some("net")
          ),
          mkSubcontractor(
            id = 2L,
            ref = Some(2002L),
            verified = Some("Y"),
            verificationDate = Some(LocalDateTime.of(2024, 6, 1, 0, 0)),
            lastMrDate = None,
            verificationNumber = Some("123457"),
            taxTreatment = Some("gross")
          )
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF =
        service.buildSelectSubcontractorPage(
          cisId = "1",
          taxMonth = 1,
          taxYear = 2026,
          defaultSelection = None,
          today = LocalDate.of(2026, 1, 29)
        )

      whenReady(modelF) { model =>
        model.initiallySelectedIds mustBe Seq(1)

        val byId = model.subcontractors.map(vm => vm.id -> vm).toMap
        byId(1).verificationRequired mustBe "Yes"
        byId(2).verificationRequired mustBe "No"

        byId(1).verificationNumber mustBe "Unknown"
        byId(1).taxTreatment mustBe "Unknown"

        byId(2).verificationNumber mustBe "123457"
        byId(2).taxTreatment mustBe "Gross"
      }
    }

    "uses ids from UserAnswers when defaultSelection is None and selectedSubcontractors is non-empty" in {
      val response = mkResponse(
        items = Seq(mkItem(2002L)),
        subs = Seq(
          mkSubcontractor(id = 1L, ref = None, verified = Some("N"), verificationDate = None, lastMrDate = None),
          mkSubcontractor(id = 2L, ref = Some(2002L), verified = Some("N"), verificationDate = None, lastMrDate = None)
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val ua = UserAnswers(
        id = "id",
        data = Json.obj(
          "subcontractors" -> Json.obj(
            "0" -> Json.toJson(
              SelectedSubcontractor(
                id = 1L,
                name = "Subbie 1",
                totalPaymentsMade = None,
                costOfMaterials = None,
                totalTaxDeducted = None
              )
            )
          )
        )
      )

      val modelF =
        service.buildSelectSubcontractorPage(
          cisId = "1",
          taxMonth = 1,
          taxYear = 2026,
          defaultSelection = None,
          userAnswers = Some(ua),
          today = LocalDate.of(2026, 1, 29)
        )

      whenReady(modelF) { model =>
        // Sub 2 was included last month, but the saved UserAnswers contain sub 1,
        // so the `case None if selectedSubcontractors.nonEmpty` branch fires and
        // returns sub 1's id rather than the last-month selection.
        model.initiallySelectedIds mustBe Seq(1)
      }
    }

    "preselects all subcontractors when defaultSelection is Some(true)" in {
      val response = mkResponse(
        items = Seq.empty,
        subs = Seq(
          mkSubcontractor(id = 1L, ref = Some(1001L), verified = Some("N"), verificationDate = None, lastMrDate = None),
          mkSubcontractor(id = 2L, ref = Some(2002L), verified = Some("N"), verificationDate = None, lastMrDate = None)
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF =
        service.buildSelectSubcontractorPage(
          cisId = "1",
          taxMonth = 1,
          taxYear = 2026,
          defaultSelection = Some(true),
          today = LocalDate.of(2026, 1, 29)
        )

      whenReady(modelF) { model =>
        model.initiallySelectedIds mustBe Seq(1, 2)
      }
    }

    "maps taxTreatment 'net' to 'Standard rate' when verification is not required" in {
      val response = mkResponse(
        items = Seq.empty,
        subs = Seq(
          mkSubcontractor(
            id = 1L,
            ref = Some(1001L),
            verified = Some("Y"),
            verificationDate = Some(LocalDateTime.of(2024, 6, 1, 0, 0)),
            lastMrDate = None,
            verificationNumber = Some("VN001"),
            taxTreatment = Some("net")
          )
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF =
        service.buildSelectSubcontractorPage(
          cisId = "1",
          taxMonth = 1,
          taxYear = 2026,
          defaultSelection = None,
          today = LocalDate.of(2026, 1, 29)
        )

      whenReady(modelF) { model =>
        model.subcontractors.head.taxTreatment mustBe "Standard rate"
      }
    }

    "maps taxTreatment 'unmatched' to 'Higher rate' when verification is not required" in {
      val response = mkResponse(
        items = Seq.empty,
        subs = Seq(
          mkSubcontractor(
            id = 1L,
            ref = Some(1001L),
            verified = Some("Y"),
            verificationDate = Some(LocalDateTime.of(2024, 6, 1, 0, 0)),
            lastMrDate = None,
            verificationNumber = Some("VN001"),
            taxTreatment = Some("unmatched")
          )
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF =
        service.buildSelectSubcontractorPage(
          cisId = "1",
          taxMonth = 1,
          taxYear = 2026,
          defaultSelection = None,
          today = LocalDate.of(2026, 1, 29)
        )

      whenReady(modelF) { model =>
        model.subcontractors.head.taxTreatment mustBe "Higher rate"
      }
    }

    "preselects no subcontractors when defaultSelection is Some(false)" in {
      val response = mkResponse(
        items = Seq(mkItem(1001L)),
        subs = Seq(
          mkSubcontractor(id = 1L, ref = Some(1001L), verified = Some("N"), verificationDate = None, lastMrDate = None),
          mkSubcontractor(id = 2L, ref = Some(2002L), verified = Some("N"), verificationDate = None, lastMrDate = None)
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF =
        service.buildSelectSubcontractorPage(
          cisId = "1",
          taxMonth = 1,
          taxYear = 2026,
          defaultSelection = Some(false),
          today = LocalDate.of(2026, 1, 29)
        )

      whenReady(modelF) { model =>
        model.initiallySelectedIds mustBe Seq.empty
      }
    }
  }

  "SubcontractorService.resolveSubcontractorName" - {

    val soleTrader  = Some(SubcontractorType.SoleTrader.value)
    val company     = Some(SubcontractorType.Company.value)
    val trust       = Some(SubcontractorType.Trust.value)
    val partnership = Some(SubcontractorType.Partnership.value)

    "returns 'firstName surname' for a sole trader with both names" in {
      val sub = mkNameSubcontractor(subcontractorType = soleTrader, firstName = Some("John"), surname = Some("Smith"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "John Smith"
    }

    "returns surname for a sole trader with only surname" in {
      val sub = mkNameSubcontractor(subcontractorType = soleTrader, surname = Some("Smith"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Smith"
    }

    "returns tradingName for a sole trader with only trading name" in {
      val sub = mkNameSubcontractor(subcontractorType = soleTrader, tradingName = Some("Smith Builders"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Smith Builders"
    }

    "returns tradingName for a company" in {
      val sub = mkNameSubcontractor(subcontractorType = company, tradingName = Some("Acme Construction"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Acme Construction"
    }

    "returns tradingName for a trust" in {
      val sub = mkNameSubcontractor(subcontractorType = trust, tradingName = Some("Big Trust"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Big Trust"
    }

    "returns partnershipTradingName for a partnership" in {
      val sub = mkNameSubcontractor(
        subcontractorType = partnership,
        partnershipTradingName = Some("Partner Co"),
        tradingName = Some("Trading Co")
      )
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Partner Co"
    }

    "returns tradingName for a partnership when only trading name is provided" in {
      val sub = mkNameSubcontractor(subcontractorType = partnership, tradingName = Some("Trading Co"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Trading Co"
    }

    "returns 'No name provided' when all name fields are null" in {
      val sub = mkNameSubcontractor(subcontractorType = soleTrader)
      SubcontractorService.resolveSubcontractorName(sub) mustBe "No name provided"
    }

    "returns 'No name provided' when subcontractorType is missing" in {
      val sub = mkNameSubcontractor(tradingName = Some("Should not matter"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "No name provided"
    }

    "returns 'No name provided' for a company with no trading name" in {
      val sub = mkNameSubcontractor(subcontractorType = company)
      SubcontractorService.resolveSubcontractorName(sub) mustBe "No name provided"
    }

    "returns 'No name provided' for an unknown subcontractor type" in {
      val sub = mkNameSubcontractor(subcontractorType = Some("unknown"), tradingName = Some("Something"))
      SubcontractorService.resolveSubcontractorName(sub) mustBe "No name provided"
    }

    "trims whitespace from name fields" in {
      val sub = mkNameSubcontractor(
        subcontractorType = soleTrader,
        firstName = Some("  John  "),
        surname = Some("  Smith  ")
      )
      SubcontractorService.resolveSubcontractorName(sub) mustBe "John Smith"
    }

    "treats blank strings as absent" in {
      val sub = mkNameSubcontractor(
        subcontractorType = soleTrader,
        firstName = Some(""),
        surname = Some("  "),
        tradingName = Some("Fallback")
      )
      SubcontractorService.resolveSubcontractorName(sub) mustBe "Fallback"
    }
  }

  "SubcontractorService.buildAmendWhichSubcontractorsPage" - {

    "returns subcontractors with resolved names and pre-selects those in monthlyReturnItems" in {
      val response = mkResponse(
        items = Seq(mkItem(1001L)),
        subs = Seq(
          mkSubcontractor(id = 1L, ref = Some(1001L), verified = Some("N"), verificationDate = None, lastMrDate = None)
            .copy(
              subcontractorType = Some(SubcontractorType.SoleTrader.value),
              firstName = Some("John"),
              surname = Some("Smith")
            ),
          mkSubcontractor(id = 2L, ref = Some(2002L), verified = Some("N"), verificationDate = None, lastMrDate = None)
            .copy(subcontractorType = Some(SubcontractorType.Company.value), tradingName = Some("Acme Ltd"))
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF = service.buildAmendWhichSubcontractorsPage(
        cisId = "1",
        taxMonth = 1,
        taxYear = 2026
      )

      whenReady(modelF) { model =>
        model.subcontractors.map(_.name) mustBe Seq("John Smith", "Acme Ltd")
        model.subcontractors.map(_.id) mustBe Seq("1", "2")
        model.preSelectedIds mustBe Set("1")
        model.status mustBe Some("STARTED")
      }
    }

    "uses user answers selection when WhichSubcontractorsToAddPage is already set" in {
      val response = mkResponse(
        items = Seq(mkItem(1001L)),
        subs = Seq(
          mkSubcontractor(id = 1L, ref = Some(1001L), verified = Some("N"), verificationDate = None, lastMrDate = None),
          mkSubcontractor(id = 2L, ref = Some(2002L), verified = Some("N"), verificationDate = None, lastMrDate = None)
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val ua = emptyUserAnswers
        .set(WhichSubcontractorsToAddPage, Set("2"))
        .success
        .value

      val modelF = service.buildAmendWhichSubcontractorsPage(
        cisId = "1",
        taxMonth = 1,
        taxYear = 2026,
        userAnswers = Some(ua)
      )

      whenReady(modelF) { model =>
        model.preSelectedIds mustBe Set("2")
        model.status mustBe Some("STARTED")
      }
    }

    "pre-selects none when no subcontractors match monthlyReturnItems" in {
      val response = mkResponse(
        items = Seq(mkItem(9999L)),
        subs = Seq(
          mkSubcontractor(id = 1L, ref = Some(1001L), verified = Some("N"), verificationDate = None, lastMrDate = None),
          mkSubcontractor(id = 2L, ref = Some(2002L), verified = Some("N"), verificationDate = None, lastMrDate = None)
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(response))

      val modelF = service.buildAmendWhichSubcontractorsPage(
        cisId = "1",
        taxMonth = 1,
        taxYear = 2026
      )

      whenReady(modelF) { model =>
        model.preSelectedIds mustBe Set.empty
        model.status mustBe Some("STARTED")
      }
    }
  }

  private def mkNameSubcontractor(
    subcontractorType: Option[String] = None,
    firstName: Option[String] = None,
    surname: Option[String] = None,
    tradingName: Option[String] = None,
    partnershipTradingName: Option[String] = None
  ): Subcontractor =
    mkSubcontractor(id = 1L, ref = None, verified = None, verificationDate = None, lastMrDate = None)
      .copy(
        subcontractorType = subcontractorType,
        firstName = firstName,
        surname = surname,
        tradingName = tradingName,
        partnershipTradingName = partnershipTradingName
      )

  private def mkResponse(items: Seq[MonthlyReturnItem], subs: Seq[Subcontractor]) =
    GetAllMonthlyReturnDetailsResponse(
      scheme = Seq.empty,
      monthlyReturn = Seq(
        MonthlyReturn(monthlyReturnId = 101, taxYear = 2026, taxMonth = 1, status = Some("STARTED"))
      ),
      subcontractors = subs,
      monthlyReturnItems = items,
      submission = Seq.empty
    )

  private def mkItem(ref: Long): MonthlyReturnItem =
    MonthlyReturnItem(
      monthlyReturnId = 1L,
      monthlyReturnItemId = ref,
      totalPayments = None,
      costOfMaterials = None,
      totalDeducted = None,
      unmatchedTaxRateIndicator = None,
      subcontractorId = None,
      subcontractorName = None,
      verificationNumber = None,
      itemResourceReference = Some(ref)
    )

  private def mkSubcontractor(
    id: Long,
    ref: Option[Long],
    verified: Option[String],
    verificationDate: Option[LocalDateTime],
    lastMrDate: Option[LocalDateTime],
    verificationNumber: Option[String] = None,
    taxTreatment: Option[String] = None
  ): Subcontractor =
    Subcontractor(
      subcontractorId = id,
      utr = None,
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = None,
      nino = None,
      secondName = None,
      surname = None,
      partnershipTradingName = None,
      tradingName = Some(s"Subbie $id"),
      subcontractorType = None,
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
      subbieResourceRef = ref,
      matched = None,
      autoVerified = None,
      verified = verified,
      verificationNumber = verificationNumber,
      taxTreatment = taxTreatment,
      verificationDate = verificationDate,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = lastMrDate,
      pendingVerifications = None,
      displayName = Some("First-name-and-Surname")
    )
}
