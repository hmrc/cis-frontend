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
import org.mockito.Mockito.when
import play.api.routing.{HandlerDef, Router}
import play.api.test.FakeRequest
import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar

class ClientListCheckPolicyResolverSpec extends SpecBase {

  private val resolver = new ClientListCheckPolicyResolver()

  private def request(
    method: String,
    controller: String,
    action: String
  ) = {
    val handlerDef = MockitoSugar.mock[HandlerDef]

    when(handlerDef.controller).thenReturn(controller)
    when(handlerDef.method).thenReturn(action)

    FakeRequest(method, "/test")
      .addAttr(Router.Attrs.HandlerDef, handlerDef)
  }

  "ClientListCheckPolicyResolver" - {

    "must return GroupA for an explicitly configured GroupA route" in {
      val result = resolver.resolve(
        request(
          "GET",
          "controllers.monthlyreturns.FileYourMonthlyCisReturnController",
          "startMonthlyReturn"
        )
      )

      result mustBe ClientListCheckPolicy.GroupA
    }

    "must return Exempt for an exempt controller" in {
      val result = resolver.resolve(
        request(
          "GET",
          "controllers.SystemErrorController",
          "onPageLoad"
        )
      )

      result mustBe ClientListCheckPolicy.Exempt
    }

    "must return GroupA for a GET onPageLoad route" in {
      val result = resolver.resolve(
        request(
          "GET",
          "controllers.SomeController",
          "onPageLoad"
        )
      )

      result mustBe ClientListCheckPolicy.GroupA
    }

    "must return Exempt for a non-GET request" in {
      val result = resolver.resolve(
        request(
          "POST",
          "controllers.SomeController",
          "onPageLoad"
        )
      )

      result mustBe ClientListCheckPolicy.Exempt
    }

    "must return Exempt for an unmatched GET route" in {
      val result = resolver.resolve(
        request(
          "GET",
          "controllers.SomeController",
          "onSubmit"
        )
      )

      result mustBe ClientListCheckPolicy.Exempt
    }
  }
}
