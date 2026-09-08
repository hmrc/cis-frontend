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

package controllers.finalvalidations

import base.SpecBase
import config.FrontendAppConfig
import connectors.ConstructionIndustrySchemeConnector
import models.finalvalidation.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.finalvalidations.FinalValidationDraftIdPage
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.finalvalidation.FinalValidationDraftService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

import play.api.Application
import play.api.mvc.{Call, Result}
import play.api.routing.Router

class FinalValidationChangeControllerSpec extends SpecBase {

  private val subcontractorId   = 1L
  private val subbieResourceRef = 100L
  private val cisId             = "1"
  private val draftId           = "draft-id"
  private val handoffId         = "handoff-id"

  private val field =
    FinalValidationField.Utr

  private val changeTarget =
    FinalValidationChangeTarget.TradingName

  private val finalValidationChangeCall =
    controllers.finalvalidations.routes.FinalValidationChangeController
      .onPageLoad(subcontractorId, field.key, changeTarget.key)

  private val finalValidationChangeRoute =
    finalValidationChangeCall.url

  private def diagnoseRoute(
    application: Application,
    call: Call,
    result: Future[Result]
  ): Unit = {

    val router =
      application.injector.instanceOf[Router]

    val request =
      FakeRequest(call.method, call.url)

    val handler =
      router.handlerFor(request)

    val configuredRouter =
      application.configuration
        .getOptional[String]("play.http.router")
        .getOrElse("<not configured>")

    val relevantRoutes =
      router.documentation.filter { case (_, path, controller) =>
        val route =
          s"$path $controller".toLowerCase

        route.contains("finalvalidation") ||
        route.contains("final-validation") ||
        controller.contains("FinalValidationChangeController")
      }

    val responseBody =
      contentAsString(result)

    println()
    println("============================================================")
    println("[FINAL-VALIDATION-ROUTE-DIAGNOSTIC]")
    println("============================================================")
    println(s"[ROUTE-DIAG] configured router     = $configuredRouter")
    println(s"[ROUTE-DIAG] actual router class   = ${router.getClass.getName}")
    println(s"[ROUTE-DIAG] reverse route method  = ${call.method}")
    println(s"[ROUTE-DIAG] reverse route url     = ${call.url}")
    println(s"[ROUTE-DIAG] request method        = ${request.method}")
    println(s"[ROUTE-DIAG] request uri           = ${request.uri}")
    println(s"[ROUTE-DIAG] request path          = ${request.path}")
    println(s"[ROUTE-DIAG] handler found         = ${handler.isDefined}")
    println(
      s"[ROUTE-DIAG] handler class         = ${handler
          .map(_.getClass.getName)
          .getOrElse("<none>")}"
    )
    println(
      s"[ROUTE-DIAG] handler               = ${handler
          .map(_.toString)
          .getOrElse("<none>")}"
    )
    println(s"[ROUTE-DIAG] route count           = ${router.documentation.size}")
    println(s"[ROUTE-DIAG] relevant route count  = ${relevantRoutes.size}")

    relevantRoutes.foreach { case (method, path, controller) =>
      println(s"[ROUTE-DIAG] registered route      = $method $path -> $controller")
    }

    println(s"[ROUTE-DIAG] response status       = ${status(result)}")
    println(
      s"[ROUTE-DIAG] response location     = ${redirectLocation(result)
          .getOrElse("<none>")}"
    )
    println(
      s"[ROUTE-DIAG] response content type = ${contentType(result)
          .getOrElse("<none>")}"
    )
    println(s"[ROUTE-DIAG] response headers      = ${headers(result)}")
    println("[ROUTE-DIAG] response body         =")
    println(responseBody.take(2000))

    if (responseBody.length > 2000) {
      println(s"[ROUTE-DIAG] response body truncated, length = ${responseBody.length}")
    }

    println("============================================================")
    println()
  }

  "FinalValidationChangeController.onPageLoad" - {

    "must create a handoff and redirect to the contractor frontend" in {
      val connector                   = mock[ConstructionIndustrySchemeConnector]
      val finalValidationDraftService = mock[FinalValidationDraftService]
      val draft                       = mock[FinalValidationDraft]
      val subcontractor               = mock[FinalValidationDraftSubcontractor]
      val issue                       = mock[FinalValidationDraftIssue]

      val userAnswers =
        emptyUserAnswers
          .set(CisIdPage, cisId)
          .success
          .value
          .set(FinalValidationDraftIdPage, draftId)
          .success
          .value

      when(
        finalValidationDraftService.get(any[String], any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(draft))

      when(draft.subcontractor(subcontractorId)).thenReturn(Some(subcontractor))
      when(subcontractor.readiness).thenReturn(FinalValidationReadiness.Incomplete)
      when(subcontractor.issues).thenReturn(Seq(issue))
      when(issue.fieldKey).thenReturn(field.key)
      when(subcontractor.subcontractorId).thenReturn(subcontractorId)
      when(subcontractor.subbieResourceRef).thenReturn(subbieResourceRef)

      when(
        connector.createJourneyHandoff(
          any(),
          any[JsObject]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(handoffId))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector),
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            finalValidationChangeCall.method,
            finalValidationChangeRoute
          )

        val result = route(application, request).value

        diagnoseRoute(
          application,
          finalValidationChangeCall,
          result
        )

        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          appConfig.cisContractorFinalValidationHandoffUrl(handoffId)
      }
    }

    "must redirect to Journey Recovery when the draft id is missing" in {
      val userAnswers =
        emptyUserAnswers
          .set(CisIdPage, cisId)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            finalValidationChangeCall.method,
            finalValidationChangeRoute
          )

        val result = route(application, request).value

        diagnoseRoute(
          application,
          finalValidationChangeCall,
          result
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the subcontractor is not in the draft" in {
      val finalValidationDraftService = mock[FinalValidationDraftService]
      val draft                       = mock[FinalValidationDraft]

      val userAnswers =
        emptyUserAnswers
          .set(CisIdPage, cisId)
          .success
          .value
          .set(FinalValidationDraftIdPage, draftId)
          .success
          .value

      when(
        finalValidationDraftService.get(any[String], any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(draft))

      when(draft.subcontractor(subcontractorId)).thenReturn(None)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            finalValidationChangeCall.method,
            finalValidationChangeRoute
          )

        val result = route(application, request).value

        diagnoseRoute(
          application,
          finalValidationChangeCall,
          result
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
