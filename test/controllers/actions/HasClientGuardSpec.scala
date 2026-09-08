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

package controllers.actions

import models.agent.AgentClientData
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AuditService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import base.SpecBase
import models.audit.AuthFailureAuditEventModel
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.http.connector.AuditResult

class HasClientGuardSpec extends SpecBase {

  private given ExecutionContext                             = ExecutionContext.global
  private val mockMonthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService]
  private val mockAuditService: AuditService                 = mock[AuditService]
  private val guard                                          = new HasClientGuard(mockMonthlyReturnService, mockAuditService)

  private def request(isAgent: Boolean): IdentifierRequest[AnyContent] =
    IdentifierRequest(
      FakeRequest(),
      "user-id",
      None,
      if isAgent then Some("agent-ref") else None,
      isAgent
    )

  private val client =
    AgentClientData(
      uniqueId = "instance-id",
      taxOfficeNumber = "123",
      taxOfficeReference = "AB456",
      schemeName = Some("Test Scheme")
    )

  when(
    mockAuditService.sendEvent(any[AuthFailureAuditEventModel])(using
      any[HeaderCarrier],
      any[Writes[AuthFailureAuditEventModel]],
      any[Request[?]]
    )
  ).thenReturn(Future.successful(AuditResult.Success))

  "HasClientGuard" - {

    "must redirect to system error when agent client data is missing" in {
      when(
        mockMonthlyReturnService
          .getAgentClient(any[String])(using any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(None))

      val result =
        guard.check(request(isAgent = true)).futureValue

      result.value.header.status mustBe SEE_OTHER
      result.value.header.headers.get(LOCATION) mustBe
        Some(controllers.routes.SystemErrorController.onPageLoad().url)
    }

    "must continue when hasClient returns true" in {
      when(
        mockMonthlyReturnService
          .getAgentClient(any[String])(using any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(Some(client)))

      when(
        mockMonthlyReturnService
          .hasClient(any[String], any[String])(using any[HeaderCarrier])
      ).thenReturn(Future.successful(true))

      val result =
        guard.check(request(isAgent = true)).futureValue

      result mustBe None
    }

    "must redirect to system error when hasClient returns false" in {
      when(
        mockMonthlyReturnService
          .getAgentClient(any[String])(using any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(Some(client)))

      when(
        mockMonthlyReturnService
          .hasClient(any[String], any[String])(using any[HeaderCarrier])
      ).thenReturn(Future.successful(false))

      val result =
        guard.check(request(isAgent = true)).futureValue

      result.value.header.status mustBe SEE_OTHER
      result.value.header.headers.get(LOCATION) mustBe
        Some(controllers.routes.SystemErrorController.onPageLoad().url)
    }

    "must redirect to system error when hasClient fails" in {
      when(
        mockMonthlyReturnService
          .getAgentClient(any[String])(using any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(Some(client)))

      when(
        mockMonthlyReturnService
          .hasClient(any[String], any[String])(using any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        guard.check(request(isAgent = true)).futureValue

      result.value.header.status mustBe SEE_OTHER
      result.value.header.headers.get(LOCATION) mustBe
        Some(controllers.routes.SystemErrorController.onPageLoad().url)
    }

    "must redirect to system error when tax office details are missing" in {
      val clientWithMissingDetails =
        client.copy(taxOfficeNumber = "")

      when(
        mockMonthlyReturnService
          .getAgentClient(any[String])(using any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(Some(clientWithMissingDetails)))

      val result =
        guard.check(request(isAgent = true)).futureValue

      result.value.header.status mustBe SEE_OTHER
    }
  }
}
