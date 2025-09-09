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
import models.responses.{InTransitMonthlyReturnDetails, MonthlyReturnResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class MonthlyReturnServiceSpec extends AnyWordSpec with MockitoSugar with ScalaFutures with Matchers {

  implicit val ec: ExecutionContext = global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConnector: ConstructionIndustrySchemeConnector =
    mock[ConstructionIndustrySchemeConnector]

  val service = new MonthlyReturnService(mockConnector)

  private def mr(year: Int, month: Int, id: Long = 1L): InTransitMonthlyReturnDetails =
    InTransitMonthlyReturnDetails(
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

  "MonthlyReturnService.retrieveAllMonthlyReturns" should {
    "return the payload from the connector and call it with the given instanceId" in {
      val expected = MonthlyReturnResponse(Seq(mr(2023, 4), mr(2024, 5)))

      when(mockConnector.retrieveMonthlyReturns()(any()))
        .thenReturn(Future.successful(expected))

      val result = service.retrieveAllMonthlyReturns().futureValue
      result mustBe expected

      verify(mockConnector, times(1))
        .retrieveMonthlyReturns()(any())
      verifyNoMoreInteractions(mockConnector)
    }

    "propagate failures from the connector" in {
      when(mockConnector.retrieveMonthlyReturns()(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val ex = intercept[RuntimeException] {
        service.retrieveAllMonthlyReturns().futureValue
      }
      ex.getMessage must include("boom")
    }
  }

  "MonthlyReturnService.isDuplicate" should {
    "return true when a monthly return with the same year and month exists" in {
      val data = MonthlyReturnResponse(Seq(mr(2022, 12), mr(2024, 5)))
      when(mockConnector.retrieveMonthlyReturns()(any()))
        .thenReturn(Future.successful(data))

      service.isDuplicate(2024, 5).futureValue mustBe true
    }

    "return false when no monthly return matches the year and month" in {
      val data = MonthlyReturnResponse(Seq(mr(2024, 4), mr(2024, 6)))
      when(mockConnector.retrieveMonthlyReturns()(any()))
        .thenReturn(Future.successful(data))

      service.isDuplicate(2024, 5).futureValue mustBe false
    }

    "propagate failures from the connector" in {
      when(mockConnector.retrieveMonthlyReturns()(any()))
        .thenReturn(Future.failed(new RuntimeException("oops")))

      val ex = intercept[RuntimeException] {
        service.isDuplicate(2024, 5).futureValue
      }
      ex.getMessage must include("oops")
    }
  }
}
