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

import models.agent.ClientListStatus
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.{ExecutionContext, Future}

class ClientListStatusGuardSpec extends SpecBase {

  private given ExecutionContext                             = ExecutionContext.global
  private val mockMonthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService]
  private val guard                                          = new ClientListStatusGuard(mockMonthlyReturnService)

  private def request(isAgent: Boolean): IdentifierRequest[AnyContent] =
    IdentifierRequest(
      FakeRequest(),
      "user-id",
      None,
      if isAgent then Some("agent-ref") else None,
      isAgent
    )

  "ClientListStatusGuard" - {

    "must bypass the check for a non-agent" in {
      val result =
        guard.checkGroupA(request(isAgent = false)).futureValue

      result mustBe None

      verify(mockMonthlyReturnService, never())
        .startClientListRetrieval(using any[HeaderCarrier])
    }

    "must continue when client list retrieval succeeds" in {
      when(
        mockMonthlyReturnService.startClientListRetrieval(using any[HeaderCarrier])
      ).thenReturn(Future.successful(ClientListStatus.Succeeded))

      val result =
        guard.checkGroupA(request(isAgent = true)).futureValue

      result mustBe None
    }

    "must redirect to system error for a non-success status" in {
      Seq(
        ClientListStatus.InProgress,
        ClientListStatus.Failed,
        ClientListStatus.InitiateDownload
      ).foreach { status =>
        when(
          mockMonthlyReturnService.startClientListRetrieval(using any[HeaderCarrier])
        ).thenReturn(Future.successful(status))

        val result =
          guard.checkGroupA(request(isAgent = true)).futureValue

        result.value.header.status mustBe SEE_OTHER
        result.value.header.headers.get(LOCATION) mustBe
          Some(controllers.routes.SystemErrorController.onPageLoad().url)
      }
    }

    "must redirect to system error when client list retrieval fails" in {
      when(
        mockMonthlyReturnService.startClientListRetrieval(using any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        guard.checkGroupA(request(isAgent = true)).futureValue

      result.value.header.status mustBe SEE_OTHER
      result.value.header.headers.get(LOCATION) mustBe
        Some(controllers.routes.SystemErrorController.onPageLoad().url)
    }
  }
}
