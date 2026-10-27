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

import base.SpecBase
import config.FrontendAppConfig
import models.requests.SchemeRequest
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContent, Results}

import scala.concurrent.{ExecutionContext, Future}

class MultiTabActionSpec extends SpecBase {

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  "MultiTabAction" - {

    "allow the request when multi-tab is enabled" in {
      val appConfig = mock[FrontendAppConfig]
      val request   = mock[SchemeRequest[AnyContent]]
      val action    = new MultiTabAction(appConfig)

      when(appConfig.multiTabEnabled).thenReturn(true)

      val result = action
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe OK
    }

    "redirect to the CIS Manage Returns dashboard when multi-tab is disabled" in {
      val appConfig  = mock[FrontendAppConfig]
      val request    = mock[SchemeRequest[AnyContent]]
      val action     = new MultiTabAction(appConfig)
      val cisId      = "123456789"
      val landingUrl = s"http://localhost:6996/construction-industry-scheme/management/manage-cis-return/$cisId"

      when(appConfig.multiTabEnabled).thenReturn(false)
      when(request.cisId).thenReturn(cisId)
      when(appConfig.returnsLandingPageUrl(cisId, None)).thenReturn(landingUrl)

      val result = action
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(landingUrl)
    }
  }
}
