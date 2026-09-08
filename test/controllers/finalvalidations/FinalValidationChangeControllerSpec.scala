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

  private val finalValidationChangeRoute =
    controllers.finalvalidations.routes.FinalValidationChangeController
      .onPageLoad(subcontractorId, field.key, changeTarget.key)
      .url

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
        applicationBuilder(userAnswers = Some(userAnswers)).build()

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
        applicationBuilder(userAnswers = Some(userAnswers))
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
}
