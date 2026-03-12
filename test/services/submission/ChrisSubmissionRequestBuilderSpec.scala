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

import connectors.ConstructionIndustrySchemeConnector
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.{ReturnType, UserAnswers}
import models.monthlyreturns.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.TryValues.*
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class ChrisSubmissionRequestBuilderSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with TryValues {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private def mkTaxpayer(): CisTaxpayer =
    CisTaxpayer(
      uniqueId = "123",
      taxOfficeNumber = "123",
      taxOfficeRef = "AB456",
      employerName1 = Some("TEST LTD"),
      utr = Some("1234567890"),
      aoReference = Some("02240"),
      aoDistrict = Some("754"),
      aoPayType = Some("PT"),
      aoCheckCode = Some("0000"),
      validBusinessAddr = None,
      correlation = None,
      ggAgentId = None,
      employerName2 = None,
      agentOwnRef = None,
      schemeName = None,
      enrolledSig = None
    )

  private def mkSubcontractor(id: Long): Subcontractor =
    Subcontractor(
      subcontractorId = id,
      utr = None,
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = Some("A"),
      nino = None,
      secondName = None,
      surname = Some("B"),
      partnershipTradingName = None,
      tradingName = Some("TRADING"),
      subcontractorType = Some("soletrader"),
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
      createDate = Some(LocalDateTime.now()),
      lastUpdate = Some(LocalDateTime.now()),
      subbieResourceRef = None,
      matched = Some("yes"),
      autoVerified = None,
      verified = None,
      verificationNumber = None,
      taxTreatment = None,
      verificationDate = None,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = None,
      pendingVerifications = None,
      displayName = Some("A B")
    )

  "ChrisSubmissionRequestBuilder.build" should {

    "build MonthlyNilReturn request (minimal)" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value
          .set(InactivityRequestPage, InactivityRequest.Option1)
          .success
          .value
          .set(ConfirmEmailAddressPage, "  test@test.com  ")
          .success
          .value

      val reqF = builder.build(ua, mkTaxpayer(), isAgent = false)

      whenReady(reqF) { req =>
        req.returnType mustBe MonthlyNilReturn
        req.standard mustBe None

        req.utr mustBe "1234567890"
        req.aoReference mustBe "754PT000002240"
        req.monthYear mustBe "2025-09"
        req.email mustBe Some("test@test.com")

        req.informationCorrect mustBe "yes"
        req.inactivity mustBe "yes"
        req.isAgent mustBe false
        req.clientTaxOfficeNumber mustBe "123"
        req.clientTaxOfficeRef mustBe "AB456"
      }

      verify(connector, times(0))
        .retrieveMonthlyReturnForEditDetails(any[String], any[Int], any[Int])(any[HeaderCarrier])
    }

    "build MonthlyStandardReturn request" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val subId: Long = 1L

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value
          .set(InactivityRequestPage, InactivityRequest.Option2)
          .success
          .value
          .set(ConfirmEmailAddressPage, "test@test.com")
          .success
          .value
          .set(EmploymentStatusDeclarationPage, true)
          .success
          .value
          .set(VerifiedStatusDeclarationPage, true)
          .success
          .value
          .set(CisIdPage, "instance-1")
          .success
          .value
          .set(
            SelectedSubcontractorPage(0),
            SelectedSubcontractor(
              id = subId,
              name = "A B",
              totalPaymentsMade = Some(BigDecimal(100)),
              costOfMaterials = Some(BigDecimal(10)),
              totalTaxDeducted = Some(BigDecimal(20))
            )
          )
          .success
          .value

      val details = GetAllMonthlyReturnDetailsResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq(mkSubcontractor(subId)),
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      when(connector.retrieveMonthlyReturnForEditDetails(eqTo("instance-1"), eqTo(9), eqTo(2025))(any[HeaderCarrier]))
        .thenReturn(Future.successful(details))

      val reqF = builder.build(ua, mkTaxpayer(), isAgent = true)

      whenReady(reqF) { req =>
        req.returnType mustBe MonthlyStandardReturn
        req.standard.isDefined mustBe true
        req.informationCorrect mustBe "yes"
        req.inactivity mustBe "no"
        req.isAgent mustBe true
      }

      verify(connector, times(1))
        .retrieveMonthlyReturnForEditDetails(eqTo("instance-1"), eqTo(9), eqTo(2025))(any[HeaderCarrier])
    }

    "fail when ReturnTypePage missing" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua = UserAnswers("id")

      val ex = intercept[RuntimeException] {
        builder.build(ua, mkTaxpayer(), isAgent = false)
      }

      ex.getMessage mustBe "ReturnType missing"
    }

    "fail when taxpayer UTR is missing" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val taxpayer = mkTaxpayer().copy(utr = None)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, taxpayer, isAgent = false)
      }

      ex.getMessage mustBe "CIS taxpayer UTR missing"
    }

    "fail when taxpayer aoDistrict is missing" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val taxpayer = mkTaxpayer().copy(aoDistrict = None)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, taxpayer, isAgent = false)
      }

      ex.getMessage mustBe "CIS taxpayer aoDistrict missing"
    }

    "fail when taxpayer aoPayType is missing" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val taxpayer = mkTaxpayer().copy(aoPayType = None)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, taxpayer, isAgent = false)
      }

      ex.getMessage mustBe "CIS taxpayer aoPayType missing"
    }

    "fail when taxpayer aoCheckCode is missing" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val taxpayer = mkTaxpayer().copy(aoCheckCode = None)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, taxpayer, isAgent = false)
      }

      ex.getMessage mustBe "CIS taxpayer aoCheckCode missing"
    }

    "fail when taxpayer aoReference is missing" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val taxpayer = mkTaxpayer().copy(aoReference = None)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, taxpayer, isAgent = false)
      }

      ex.getMessage mustBe "CIS taxpayer aoReference missing"
    }

    "fail when month and year of return are missing for nil return" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, mkTaxpayer(), isAgent = false)
      }

      ex.getMessage mustBe "Month and year of return missing"
    }

    "fail when month and year of return are missing for standard return" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, mkTaxpayer(), isAgent = false)
      }

      ex.getMessage mustBe "Month and year of return missing"
    }

    "fail when employment status declaration is missing for standard return" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value
          .set(VerifiedStatusDeclarationPage, true)
          .success
          .value
          .set(CisIdPage, "instance-1")
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, mkTaxpayer(), isAgent = false)
      }

      ex.getMessage mustBe "Employment status declaration missing"
    }

    "fail when verification answer is missing for standard return" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value
          .set(EmploymentStatusDeclarationPage, true)
          .success
          .value
          .set(CisIdPage, "instance-1")
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, mkTaxpayer(), isAgent = false)
      }

      ex.getMessage mustBe "Verification answer missing"
    }

    "fail when CIS ID is missing for standard return" in {
      val connector = mock[ConstructionIndustrySchemeConnector]
      val builder   = new ChrisSubmissionRequestBuilder(connector)

      val ua =
        UserAnswers("id")
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 9, 1))
          .success
          .value
          .set(EmploymentStatusDeclarationPage, true)
          .success
          .value
          .set(VerifiedStatusDeclarationPage, true)
          .success
          .value

      val ex = intercept[RuntimeException] {
        builder.build(ua, mkTaxpayer(), isAgent = false)
      }

      ex.getMessage mustBe "CIS ID missing"
    }
  }
}
