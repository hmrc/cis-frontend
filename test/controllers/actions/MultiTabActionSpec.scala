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
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContent, Request, Results}
import models.requests.{DataRequest, IdentifierRequest}
import models.monthlyreturns.CisTaxpayer
import models.EmployerReference
import pages.monthlyreturns.CisIdPage
import java.util.UUID
import services.CisTaxpayerService

import scala.concurrent.{ExecutionContext, Future}

class MultiTabActionSpec extends SpecBase {

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private val cisId = "123456789"

  private def dataRequest(
    userAnswers: models.UserAnswers,
    isAgent: Boolean = false
  ): DataRequest[AnyContent] =
    DataRequest(
      mock[Request[AnyContent]],
      userAnswersId,
      userAnswers,
      Option.when(!isAgent)(EmployerReference("123", "AB456")),
      None,
      isAgent,
      None
    )

  private def mockTaxpayer(uniqueId: String): CisTaxpayer = {
    val taxpayer = mock[CisTaxpayer]

    when(taxpayer.uniqueId).thenReturn(uniqueId)

    taxpayer
  }

  "MultiTabAction.forJourney" - {

    "allow the request when the UUID-scoped CIS ID is authorised and multi-tab is enabled" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val taxpayer           = mockTaxpayer(cisId)
      val flowId             = UUID.randomUUID()

      val userAnswers = emptyUserAnswers
        .set(CisIdPage(flowId.toString), cisId)
        .get

      val request = dataRequest(userAnswers)

      when(appConfig.multiTabEnabled).thenReturn(true)
      when(cisTaxpayerService.findForOrganisation(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(taxpayer))

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe OK
    }

    "redirect to the CIS Manage Returns dashboard when multi-tab is disabled" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val taxpayer           = mockTaxpayer(cisId)
      val flowId             = UUID.randomUUID()
      val landingUrl         = s"http://localhost:6996/construction-industry-scheme/management/manage-cis-return/$cisId"

      val userAnswers = emptyUserAnswers
        .set(CisIdPage(flowId.toString), cisId)
        .get

      val request = dataRequest(userAnswers)

      when(appConfig.multiTabEnabled).thenReturn(false)
      when(appConfig.returnsLandingPageUrl(cisId, None)).thenReturn(landingUrl)
      when(cisTaxpayerService.findForOrganisation(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(taxpayer))

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(landingUrl)
    }

    "redirect to Journey Recovery when the UUID-scoped CIS ID is missing" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val flowId             = UUID.randomUUID()
      val request            = dataRequest(emptyUserAnswers)

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe
        Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)

      verifyNoInteractions(cisTaxpayerService)
    }

    "not fall back to the legacy CIS ID when the UUID-scoped CIS ID is missing" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val flowId             = UUID.randomUUID()

      val userAnswers = emptyUserAnswers
        .set(CisIdPage, cisId)
        .get

      val request = dataRequest(userAnswers)

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe
        Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)

      verifyNoInteractions(cisTaxpayerService)
    }

    "redirect to Journey Recovery when the scoped CIS ID does not belong to the organisation" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val taxpayer           = mockTaxpayer(cisId)
      val flowId             = UUID.randomUUID()

      val userAnswers = emptyUserAnswers
        .set(CisIdPage(flowId.toString), "987654321")
        .get

      val request = dataRequest(userAnswers)

      when(appConfig.multiTabEnabled).thenReturn(true)
      when(cisTaxpayerService.findForOrganisation(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(taxpayer))

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe
        Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
    }

    "allow the request when the UUID-scoped CIS ID belongs to an authorised agent client" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val taxpayer           = mockTaxpayer(cisId)
      val flowId             = UUID.randomUUID()

      val userAnswers = emptyUserAnswers
        .set(CisIdPage(flowId.toString), cisId)
        .get

      val request = dataRequest(userAnswers, isAgent = true)

      when(appConfig.multiTabEnabled).thenReturn(true)
      when(cisTaxpayerService.findForAgent(eqTo(cisId))(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(Some(taxpayer)))

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe OK

      verify(cisTaxpayerService)
        .findForAgent(eqTo(cisId))(using any[IdentifierRequest[?]])
    }

    "redirect to Journey Recovery when the agent is not authorised for the UUID-scoped CIS ID" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val flowId             = UUID.randomUUID()

      val userAnswers = emptyUserAnswers
        .set(CisIdPage(flowId.toString), cisId)
        .get

      val request = dataRequest(userAnswers, isAgent = true)

      when(appConfig.multiTabEnabled).thenReturn(true)
      when(cisTaxpayerService.findForAgent(eqTo(cisId))(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(None))

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe
        Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
    }

    "redirect to Journey Recovery when CIS taxpayer lookup fails" in {
      val appConfig          = mock[FrontendAppConfig]
      val cisTaxpayerService = mock[CisTaxpayerService]
      val action             = new MultiTabAction(appConfig, cisTaxpayerService)
      val flowId             = UUID.randomUUID()

      val userAnswers = emptyUserAnswers
        .set(CisIdPage(flowId.toString), cisId)
        .get

      val request = dataRequest(userAnswers)

      when(appConfig.multiTabEnabled).thenReturn(true)
      when(cisTaxpayerService.findForOrganisation(using any[IdentifierRequest[?]]))
        .thenReturn(Future.failed(new RuntimeException("Lookup failed")))

      val result = action
        .forJourney(flowId)
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe
        Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
    }
  }
}
