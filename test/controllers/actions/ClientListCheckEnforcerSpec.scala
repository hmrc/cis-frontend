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

import models.agent.ClientListCheckPolicy
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.{ExecutionContext, Future}

class ClientListCheckEnforcerSpec extends SpecBase {

  private given ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val mockPolicyResolver        = mock[ClientListCheckPolicyResolver]
  private val mockClientListStatusGuard = mock[ClientListStatusGuard]
  private val mockHasClientGuard        = mock[HasClientGuard]

  private val enforcer =
    new ClientListCheckEnforcer(
      mockPolicyResolver,
      mockClientListStatusGuard,
      mockHasClientGuard
    )

  private def request(isAgent: Boolean): IdentifierRequest[AnyContent] =
    IdentifierRequest(
      FakeRequest(),
      "user-id",
      None,
      if isAgent then Some("agent-ref") else None,
      isAgent
    )

  private val block: IdentifierRequest[AnyContent] => Future[Result] =
    _ => Future.successful(Results.Ok)

  "ClientListCheckEnforcer" - {

    "must bypass checks for a non-agent" in {
      val result = enforcer(request(isAgent = false))(block)

      status(result) mustBe OK

      verify(mockPolicyResolver, never()).resolve(any())
    }

    "must allow an exempt agent request through" in {
      val agentRequest = request(isAgent = true)

      when(mockPolicyResolver.resolve(agentRequest))
        .thenReturn(ClientListCheckPolicy.Exempt)

      val result = enforcer(agentRequest)(block)

      status(result) mustBe OK

      verify(mockClientListStatusGuard, never()).checkGroupA(any())
      verify(mockHasClientGuard, never()).check(any())
    }

    "must return the clientListStatus check  result when clientListStatus check  blocks the request" in {
      val agentRequest = request(isAgent = true)
      val f8Result     = Results.Redirect("/f8-failure")

      when(mockPolicyResolver.resolve(agentRequest))
        .thenReturn(ClientListCheckPolicy.GroupA)

      when(mockClientListStatusGuard.checkGroupA(agentRequest))
        .thenReturn(Future.successful(Some(f8Result)))

      val result = enforcer(agentRequest)(block)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/f8-failure")

      verify(mockHasClientGuard, never()).check(any())
    }

    "must return the hasClient check  result when clientListStatus check  succeeds but hasClient check  blocks the request" in {
      val agentRequest = request(isAgent = true)
      val f7Result     = Results.Redirect("/system-error")

      when(mockPolicyResolver.resolve(agentRequest))
        .thenReturn(ClientListCheckPolicy.GroupA)

      when(mockClientListStatusGuard.checkGroupA(agentRequest))
        .thenReturn(Future.successful(None))

      when(mockHasClientGuard.check(agentRequest))
        .thenReturn(Future.successful(Some(f7Result)))

      val result = enforcer(agentRequest)(block)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/system-error")
    }

    "must allow the request through when clientListStatus check  and hasClient check  both succeed" in {
      val agentRequest = request(isAgent = true)

      when(mockPolicyResolver.resolve(agentRequest))
        .thenReturn(ClientListCheckPolicy.GroupA)

      when(mockClientListStatusGuard.checkGroupA(agentRequest))
        .thenReturn(Future.successful(None))

      when(mockHasClientGuard.check(agentRequest))
        .thenReturn(Future.successful(None))

      val result = enforcer(agentRequest)(block)

      status(result) mustBe OK
    }
  }
}
