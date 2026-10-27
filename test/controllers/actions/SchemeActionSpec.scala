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
import models.monthlyreturns.CisTaxpayer
import models.requests.CisPath.{CisId, CisOrg}
import models.requests.IdentifierRequest
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import services.CisTaxpayerService
import views.html.PageNotFoundView

import scala.concurrent.{ExecutionContext, Future}

class SchemeActionSpec extends SpecBase {

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val cisId = "123456789"

  private def identifierRequest(isAgent: Boolean = false, uri: String = "/monthly-return") =
    IdentifierRequest(
      FakeRequest(GET, uri),
      userAnswersId,
      None,
      None,
      isAgent = isAgent
    )

  private def cisTaxpayer = {
    val taxpayer = mock(classOf[CisTaxpayer])
    when(taxpayer.uniqueId).thenReturn(cisId)
    taxpayer
  }

  private def schemeAction(
    config: FrontendAppConfig,
    service: CisTaxpayerService
  ): SchemeActionImpl =
    new SchemeActionImpl(
      app.injector.instanceOf[MessagesApi],
      config,
      service,
      mock(classOf[PageNotFoundView]),
      new MultiTabAction(config)
    )

  "SchemeAction" - {

    "return SchemeRequest for an organisation when multi-tab is enabled" in {
      val config   = mock(classOf[FrontendAppConfig])
      val service  = mock(classOf[CisTaxpayerService])
      val request  = identifierRequest()
      val taxpayer = cisTaxpayer

      when(config.multiTabEnabled).thenReturn(true)
      when(service.findForOrganisation(using request)).thenReturn(Future.successful(taxpayer))

      val result = schemeAction(config, service)(CisOrg)
        .invokeBlock(
          request,
          schemeRequest => {
            schemeRequest.cisId mustBe cisId
            schemeRequest.cisPath mustBe CisOrg
            Future.successful(Results.Ok)
          }
        )
        .futureValue

      result.header.status mustBe OK
    }

    "redirect to CIS Manage Returns when multi-tab is disabled" in {
      val config     = mock(classOf[FrontendAppConfig])
      val service    = mock(classOf[CisTaxpayerService])
      val request    = identifierRequest()
      val taxpayer   = cisTaxpayer
      val landingUrl = s"http://localhost:6996/construction-industry-scheme/management/manage-cis-return/$cisId"

      when(config.multiTabEnabled).thenReturn(false)
      when(config.returnsLandingPageUrl(cisId, None)).thenReturn(landingUrl)
      when(service.findForOrganisation(using request)).thenReturn(Future.successful(taxpayer))

      val result = schemeAction(config, service)(CisOrg)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(landingUrl)
    }

    "return SchemeRequest for an authorised agent client" in {
      val config   = mock(classOf[FrontendAppConfig])
      val service  = mock(classOf[CisTaxpayerService])
      val request  = identifierRequest(isAgent = true)
      val taxpayer = cisTaxpayer

      when(config.multiTabEnabled).thenReturn(true)
      when(service.findForAgent(cisId)(using request)).thenReturn(Future.successful(Some(taxpayer)))

      val result = schemeAction(config, service)(CisId(cisId))
        .invokeBlock(
          request,
          schemeRequest => {
            schemeRequest.cisId mustBe cisId
            schemeRequest.cisPath mustBe CisId(cisId)
            Future.successful(Results.Ok)
          }
        )
        .futureValue

      result.header.status mustBe OK
    }

    "redirect an agent accessing the organisation path" in {
      val config       = mock(classOf[FrontendAppConfig])
      val service      = mock(classOf[CisTaxpayerService])
      val request      = identifierRequest(isAgent = true)
      val agentLanding = "http://localhost:6996/agent"

      when(config.constructionIndustryAgentAccountUrl).thenReturn(agentLanding)

      val result = schemeAction(config, service)(CisOrg)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(agentLanding)
    }

    "redirect an organisation accessing an agent client path" in {
      val config  = mock(classOf[FrontendAppConfig])
      val service = mock(classOf[CisTaxpayerService])
      val request = identifierRequest(
        uri = s"/monthly-return/date-confirm-payments/$cisId/flow-id"
      )

      val result = schemeAction(config, service)(CisId(cisId))
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(
        "/monthly-return/date-confirm-payments/org/flow-id"
      )
    }
  }
}
