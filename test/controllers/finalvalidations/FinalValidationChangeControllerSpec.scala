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
import models.UserAnswers
import models.finalvalidation.FinalValidationHandoffPayload.given
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.finalvalidations.FinalValidationDraftIdPage
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.finalvalidation.FinalValidationDraftService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

class FinalValidationChangeControllerSpec extends SpecBase {

  private val subcontractorId   = 1L
  private val subbieResourceRef = 100L
  private val cisId             = "1"
  private val draftId           = "draft-id"
  private val handoffId         = "handoff-id"

  private val field = FinalValidationField.Utr

  private val changeTarget = FinalValidationChangeTarget.TradingName

  private def finalValidationChangeRoute =
    controllers.finalvalidations.routes.FinalValidationChangeController
      .onPageLoad(subcontractorId, field.key, changeTarget.key)
      .url

  private def testApplicationBuilder(
    userAnswers: Option[UserAnswers],
    multiTabEnabled: Boolean = false
  ) =
    applicationBuilder(userAnswers = userAnswers)
      .configure(
        "play.http.context"          -> "/",
        "features.multi-tab-enabled" -> multiTabEnabled
      )

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
        testApplicationBuilder(Some(userAnswers))
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector),
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request   = FakeRequest(GET, finalValidationChangeRoute)
        val result    = route(application, request).value
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
        testApplicationBuilder(Some(userAnswers))
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationChangeRoute)
        val result  = route(application, request).value

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
        testApplicationBuilder(Some(userAnswers))
          .overrides(
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationChangeRoute)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  // Multi-tab
  "FinalValidationChangeController.onPageLoadWithId" - {

    val flowId        = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val id            = flowId.toString
    val scopedCisId   = "1045"
    val scopedDraftId = "scoped-draft-id"

    def finalValidationChangeRouteWithId =
      controllers.finalvalidations.routes.FinalValidationChangeController
        .onPageLoadWithId(flowId, subcontractorId, field.key, changeTarget.key)
        .url

    val scopedUserAnswers =
      emptyUserAnswers
        .set(CisIdPage, cisId)
        .success
        .value
        .set(FinalValidationDraftIdPage, draftId)
        .success
        .value
        .set(CisIdPage(id), scopedCisId)
        .success
        .value
        .set(FinalValidationDraftIdPage(id), scopedDraftId)
        .success
        .value

    "must create a handoff using the scoped cisId, draftId and flowId and redirect to the contractor frontend" in {
      val connector                   = mock[ConstructionIndustrySchemeConnector]
      val finalValidationDraftService = mock[FinalValidationDraftService]
      val draft                       = mock[FinalValidationDraft]
      val subcontractor               = mock[FinalValidationDraftSubcontractor]
      val issue                       = mock[FinalValidationDraftIssue]

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

      val expectedPayload =
        FinalValidationHandoffPayload(
          draftId = scopedDraftId,
          instanceId = scopedCisId,
          subcontractorId = subcontractorId,
          subbieResourceRef = subbieResourceRef,
          field = field,
          changeTarget = changeTarget,
          flowId = Some(id)
        )

      val application =
        testApplicationBuilder(Some(scopedUserAnswers), multiTabEnabled = true)
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector),
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request   = FakeRequest(GET, finalValidationChangeRouteWithId)
        val result    = route(application, request).value
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          appConfig.cisContractorFinalValidationHandoffUrl(handoffId)

        val cisIdCaptor   = ArgumentCaptor.forClass(classOf[String])
        val draftIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val payloadCaptor = ArgumentCaptor.forClass(classOf[JsObject])

        verify(finalValidationDraftService)
          .get(cisIdCaptor.capture(), draftIdCaptor.capture())(any[HeaderCarrier])

        cisIdCaptor.getValue mustBe scopedCisId
        draftIdCaptor.getValue mustBe scopedDraftId

        verify(connector)
          .createJourneyHandoff(
            any(),
            payloadCaptor.capture()
          )(any[HeaderCarrier])

        payloadCaptor.getValue mustBe
          Json.toJsObject[FinalValidationHandoffPayload](expectedPayload)
      }
    }

    "must redirect to Journey Recovery when the draft id is missing under the flowId" in {
      val connector                   = mock[ConstructionIndustrySchemeConnector]
      val finalValidationDraftService = mock[FinalValidationDraftService]

      val userAnswers =
        scopedUserAnswers
          .remove(FinalValidationDraftIdPage(id))
          .success
          .value

      val application =
        testApplicationBuilder(Some(userAnswers), multiTabEnabled = true)
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector),
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationChangeRouteWithId)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(finalValidationDraftService)
        verifyNoInteractions(connector)
      }
    }

    "must redirect to Journey Recovery when the subcontractor is not in the scoped draft" in {
      val connector                   = mock[ConstructionIndustrySchemeConnector]
      val finalValidationDraftService = mock[FinalValidationDraftService]
      val draft                       = mock[FinalValidationDraft]

      when(
        finalValidationDraftService.get(any[String], any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(draft))

      when(draft.subcontractor(subcontractorId)).thenReturn(None)

      val application =
        testApplicationBuilder(Some(scopedUserAnswers), multiTabEnabled = true)
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector),
            bind[FinalValidationDraftService].toInstance(finalValidationDraftService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationChangeRouteWithId)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        val cisIdCaptor   = ArgumentCaptor.forClass(classOf[String])
        val draftIdCaptor = ArgumentCaptor.forClass(classOf[String])

        verify(finalValidationDraftService)
          .get(cisIdCaptor.capture(), draftIdCaptor.capture())(any[HeaderCarrier])

        cisIdCaptor.getValue mustBe scopedCisId
        draftIdCaptor.getValue mustBe scopedDraftId

        verifyNoInteractions(connector)
      }
    }
  }
}
