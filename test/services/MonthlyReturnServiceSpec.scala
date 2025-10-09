/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.ConstructionIndustrySchemeConnector
import models.ChrisResult.*
import models.{ChrisSubmissionRequest, UserAnswers}
import models.monthlyreturns.{CisTaxpayer, InactivityRequest, MonthlyReturnDetails, MonthlyReturnResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, InactivityRequestPage}
import repositories.SessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import java.time.{LocalDate, YearMonth}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

class MonthlyReturnServiceSpec extends AnyWordSpec with ScalaFutures with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def newService(): (MonthlyReturnService, ConstructionIndustrySchemeConnector, SessionRepository) = {
    val connector   = mock(classOf[ConstructionIndustrySchemeConnector])
    val sessionRepo = mock(classOf[SessionRepository])
    val service     = new MonthlyReturnService(connector, sessionRepo)
    (service, connector, sessionRepo)
  }

  private def createMonthlyReturn(year: Int, month: Int, id: Long): MonthlyReturnDetails =
    MonthlyReturnDetails(
      monthlyReturnId = id,
      taxYear = year,
      taxMonth = month,
      nilReturnIndicator = None,
      decEmpStatusConsidered = None,
      decAllSubsVerified = None,
      decInformationCorrect = None,
      decNoMoreSubPayments = None,
      decNilReturnNoPayments = None,
      status = None,
      lastUpdate = None,
      amendment = None,
      supersededBy = None
    )

  private def createTaxpayer(
    id: String = "CIS-123",
    ton: String = "111",
    tor: String = "test111",
    name1: Option[String] = Some("TEST LTD")
  ): CisTaxpayer =
    CisTaxpayer(
      uniqueId = id,
      taxOfficeNumber = ton,
      taxOfficeRef = tor,
      aoDistrict = None,
      aoPayType = None,
      aoCheckCode = None,
      aoReference = None,
      validBusinessAddr = None,
      correlation = None,
      ggAgentId = None,
      employerName1 = name1,
      employerName2 = None,
      agentOwnRef = None,
      schemeName = None,
      utr = None,
      enrolledSig = None
    )

  "resolveAndStoreCisId" should {

    "return existing cisId from UserAnswers without calling BE" in {
      val (service, connector, sessionRepo) = newService()

      val existing    = "CIS-001"
      val emptyUa     = UserAnswers("test-user")
      val uaWithCisId = emptyUa.set(CisIdPage, existing).get

      val (cisId, savedUa) = service.resolveAndStoreCisId(uaWithCisId).futureValue
      cisId mustBe existing
      savedUa mustBe uaWithCisId

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepo)
    }

    "fetch taxpayer when missing, store cisId in session, and return updated UA" in {
      val (service, connector, sessionRepo) = newService()

      val emptyUa  = UserAnswers("test-user")
      val taxpayer = createTaxpayer()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))
      when(sessionRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val (cisId, savedUa) = service.resolveAndStoreCisId(emptyUa).futureValue

      cisId mustBe "CIS-123"
      savedUa.get(CisIdPage) mustBe Some("CIS-123")

      val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepo).set(uaCaptor.capture())
      uaCaptor.getValue.get(CisIdPage) mustBe Some("CIS-123")

      verify(connector).getCisTaxpayer()(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "fail when BE returns empty uniqueId" in {
      val (service, connector, sessionRepo) = newService()
      val emptyUa                           = UserAnswers("test-user")

      val emptyTaxpayer = createTaxpayer(id = " ", name1 = None)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(emptyTaxpayer))

      val ex = intercept[RuntimeException] {
        service.resolveAndStoreCisId(emptyUa).futureValue
      }
      ex.getMessage must include("Empty cisId (uniqueId) returned from /cis/taxpayer")

      verify(connector).getCisTaxpayer()(any[HeaderCarrier])
      verifyNoInteractions(sessionRepo)
    }

    "fail when adding cisId to UserAnswers returns an error" in {
      val (service, connector, sessionRepo) = newService()

      val taxpayer = createTaxpayer()
      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val ua = mock(classOf[UserAnswers])
      when(ua.get(CisIdPage)).thenReturn(None)
      when(ua.set(CisIdPage, "CIS-123"))
        .thenReturn(Failure(new RuntimeException("UA set failed")))

      val ex = intercept[RuntimeException] {
        service.resolveAndStoreCisId(ua).futureValue
      }
      ex.getMessage must include("UA set failed")

      verifyNoInteractions(sessionRepo)
      verify(connector).getCisTaxpayer()(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  "retrieveAllMonthlyReturns" should {

    "delegate to connector with the given cisId and return the payload" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      val payload = MonthlyReturnResponse(
        Seq(
          createMonthlyReturn(year = 2024, month = 4, id = 101L),
          createMonthlyReturn(year = 2024, month = 5, id = 102L)
        )
      )

      when(connector.retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(payload))

      val result = service.retrieveAllMonthlyReturns(cisId).futureValue
      result mustBe payload

      verify(connector).retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "propagate failures from the connector" in {
      val (service, connector, _) = newService()

      when(connector.retrieveMonthlyReturns(eqTo("CIS-ERR"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("upstream failed")))

      val ex = intercept[RuntimeException] {
        service.retrieveAllMonthlyReturns("CIS-ERR").futureValue
      }
      ex.getMessage must include("upstream failed")
    }
  }

  "isDuplicate" should {

    "return true when a monthly return with the same (year, month) exists" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      val fetchedMonthlyReturns = MonthlyReturnResponse(
        Seq(
          createMonthlyReturn(year = 2025, month = 9, id = 1L),
          createMonthlyReturn(year = 2024, month = 5, id = 2L)
        )
      )

      when(connector.retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(fetchedMonthlyReturns))

      service.isDuplicate(cisId, year = 2024, month = 5).futureValue mustBe true
    }

    "return false when no monthly return matches the (year, month)" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      val fetchedMonthlyReturns = MonthlyReturnResponse(
        Seq(
          createMonthlyReturn(year = 2024, month = 4, id = 1L),
          createMonthlyReturn(year = 2024, month = 6, id = 2L)
        )
      )

      when(connector.retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(fetchedMonthlyReturns))

      service.isDuplicate(cisId, year = 2024, month = 5).futureValue mustBe false
    }

    "propagate failures from the connector" in {
      val (service, connector, _) = newService()

      when(connector.retrieveMonthlyReturns(eqTo("CIS-123"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val ex = intercept[RuntimeException] {
        service.isDuplicate("CIS-123", year = 2024, month = 5).futureValue
      }
      ex.getMessage must include("boom")
    }
  }

  "submitNilMonthlyReturn" should {

    def uaWith(inact: InactivityRequest, yearMonth: YearMonth): UserAnswers = {
      val emptyUa   = UserAnswers("test-user")
      val updatedUa = emptyUa.set(InactivityRequestPage, inact).get
      val date      = LocalDate.of(yearMonth.getYear, yearMonth.getMonthValue, 1)
      updatedUa.set(DateConfirmNilPaymentsPage, date).get
    }

    def taxpayerWith(utr: Option[String], aoRef: Option[String]): CisTaxpayer =
      createTaxpayer().copy(utr = utr, aoReference = aoRef)

    "build DTO correctly and return true on success" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("  1234567890 "), aoRef = Some(" 123/AB456 "))))

      val dtoCaptor: ArgumentCaptor[ChrisSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[ChrisSubmissionRequest])

      when(connector.submitChris(dtoCaptor.capture())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(200, "<Ack/>"))))

      val yearMonth = YearMonth.of(2025, 3)
      val ua        = uaWith(InactivityRequest.Option1, yearMonth)

      val result = service.submitNilMonthlyReturn(ua).futureValue
      result mustBe Submitted

      val dto = dtoCaptor.getValue
      dto.utr mustBe "1234567890"
      dto.aoReference mustBe "123/AB456"
      dto.informationCorrect mustBe "yes"
      dto.inactivity mustBe "yes"
      dto.monthYear mustBe yearMonth.toString
    }

    "map Option2 -> inactivity=false" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = Some("123/AB456"))))

      val dtoCaptor: ArgumentCaptor[ChrisSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[ChrisSubmissionRequest])

      when(connector.submitChris(dtoCaptor.capture())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(202, "<Ack/>"))))

      val yearMonth = YearMonth.of(2024, 12)
      val ua        = uaWith(InactivityRequest.Option2, yearMonth)

      val result = service.submitNilMonthlyReturn(ua).futureValue
      result mustBe Submitted
      dtoCaptor.getValue.inactivity mustBe "no"
    }

    "returns Rejected on non-2xx (e.g. 304)" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = Some("123/AB456"))))

      when(connector.submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(304, "not modified"))))

      val ua     = uaWith(InactivityRequest.Option1, YearMonth.of(2025, 7))
      val result = service.submitNilMonthlyReturn(ua).futureValue

      result mustBe Rejected(304, "not modified")
    }

    "returns Rejected on 4xx (e.g. 404)" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = Some("123/AB456"))))

      when(connector.submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("not found", 404))))

      val ua     = uaWith(InactivityRequest.Option1, YearMonth.of(2025, 7))
      val result = service.submitNilMonthlyReturn(ua).futureValue

      result mustBe UpstreamFailed(404, "not found")
    }

    "returns UpstreamFailed when connector yields UpstreamErrorResponse (e.g. 502)" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = Some("123/AB456"))))

      when(connector.submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("bad gateway", 502))))

      val ua     = uaWith(InactivityRequest.Option1, YearMonth.of(2025, 5))
      val result = service.submitNilMonthlyReturn(ua).futureValue

      result mustBe UpstreamFailed(502, "bad gateway")
    }

    "fails the future and does not call submitChris when UTR missing/empty" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = None, aoRef = Some("123/AB456"))))

      val ua     = uaWith(InactivityRequest.Option1, YearMonth.of(2025, 1))
      val result = service.submitNilMonthlyReturn(ua).failed.futureValue

      result.getMessage.toLowerCase must include("utr")
      verify(connector, never()).submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }

    "fails the future and does not call submitChris when AO reference missing/empty" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = None)))

      val ua     = uaWith(InactivityRequest.Option1, YearMonth.of(2025, 2))
      val result = service.submitNilMonthlyReturn(ua).failed.futureValue

      result.getMessage.toLowerCase must include("aoref")
      verify(connector, never()).submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }

    "fails the future when InactivityRequest was not answered" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = Some("123/AB456"))))

      val uaMissingInactivity = {
        val emptyUa = UserAnswers("test-user")
        val date    = LocalDate.of(2025, 6, 1)
        emptyUa.set(DateConfirmNilPaymentsPage, date).get
      }

      val result = service.submitNilMonthlyReturn(uaMissingInactivity).failed.futureValue
      result.getMessage.toLowerCase must include("inactivityrequest")
      verify(connector, never()).submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }

    "fails the future when Month/Year was not answered" in {
      val (service, connector, _) = newService()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayerWith(utr = Some("1234567890"), aoRef = Some("123/AB456"))))

      val uaMissingDate = {
        val emptyUa = UserAnswers("test-user")
        emptyUa.set(InactivityRequestPage, InactivityRequest.Option1).get
      }

      val result = service.submitNilMonthlyReturn(uaMissingDate).failed.futureValue
      result.getMessage.toLowerCase must include("month/year")
      verify(connector, never()).submitChris(any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }
  }
}
