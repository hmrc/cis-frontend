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
import models.monthlyreturns.{GetAllMonthlyReturnDetailsResponse, MonthlyReturnItem, Subcontractor}
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
            lastMrDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0))
          ),
          mkSubcontractor(
            id = 2L,
            ref = Some(2002L),
            verified = Some("Y"),
            verificationDate = Some(LocalDateTime.of(2024, 6, 1, 0, 0)),
            lastMrDate = None
          )
        )
      )

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(any[String], any[Int], any[Int])(any[HeaderCarrier])
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
      }
    }
  }

  private def mkResponse(items: Seq[MonthlyReturnItem], subs: Seq[Subcontractor]) =
    GetAllMonthlyReturnDetailsResponse(
      scheme = Seq.empty,
      monthlyReturn = Seq.empty,
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
    lastMrDate: Option[LocalDateTime]
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
      verificationNumber = None,
      taxTreatment = None,
      verificationDate = verificationDate,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = lastMrDate,
      pendingVerifications = None
    )
}
