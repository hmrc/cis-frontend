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
import connectors.ConstructionIndustrySchemeConnector
import models.finalvalidation.*
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.finalvalidations.FinalValidationDraftIdPage
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.libs.json.Reads
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future
import scala.reflect.ClassTag

class FinalValidationReturnControllerSpec extends SpecBase {

  private val cisId           = "1"
  private val draftId         = "draft-id"
  private val handoffId       = "handoff-id"
  private val subcontractorId = 101L

  private val payload =
    FinalValidationHandoffPayload(
      draftId = draftId,
      instanceId = cisId,
      subcontractorId = subcontractorId,
      subbieResourceRef = 100L,
      field = FinalValidationField.Utr,
      changeTarget = FinalValidationChangeTarget.TradingName
    )

  private val userAnswers =
    emptyUserAnswers
      .set(CisIdPage, cisId)
      .success
      .value
      .set(FinalValidationDraftIdPage, draftId)
      .success
      .value

  private def finalValidationReturnRoute =
    routes.FinalValidationReturnController.onPageLoad(handoffId).url

  private def testApplicationBuilder(
    userAnswers: Option[UserAnswers],
    multiTabEnabled: Boolean = false
  ) =
    applicationBuilder(userAnswers = userAnswers)
      .configure(
        "play.http.context"          -> "/",
        "features.multi-tab-enabled" -> multiTabEnabled
      )

  "FinalValidationReturnController.onPageLoad" - {

    "must delete the handoff and redirect to Update Subcontractor Details for a valid handoff" in {
      val connector = mock[ConstructionIndustrySchemeConnector]

      when(
        connector.getJourneyHandoff[FinalValidationHandoffPayload](
          any(),
          any[String]
        )(using
          any[Reads[FinalValidationHandoffPayload]],
          any[ClassTag[FinalValidationHandoffPayload]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Some(payload)))

      doReturn(Future.unit)
        .when(connector)
        .deleteJourneyHandoff(any(), any[String])(using any[HeaderCarrier])

      val application =
        testApplicationBuilder(Some(userAnswers))
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationReturnRoute)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          routes.UpdateSubcontractorDetailsController.onPageLoad(subcontractorId).url
      }
    }

    "must redirect to Journey Recovery when the handoff correlation is invalid" in {
      val connector = mock[ConstructionIndustrySchemeConnector]

      when(
        connector.getJourneyHandoff[FinalValidationHandoffPayload](
          any(),
          any[String]
        )(using
          any[Reads[FinalValidationHandoffPayload]],
          any[ClassTag[FinalValidationHandoffPayload]],
          any[HeaderCarrier]
        )
      ).thenReturn(
        Future.successful(
          Some(payload.copy(instanceId = "different-cis-id"))
        )
      )

      val application =
        testApplicationBuilder(Some(userAnswers))
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationReturnRoute)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the handoff does not exist" in {
      val connector = mock[ConstructionIndustrySchemeConnector]

      when(
        connector.getJourneyHandoff[FinalValidationHandoffPayload](
          any(),
          any[String]
        )(using
          any[Reads[FinalValidationHandoffPayload]],
          any[ClassTag[FinalValidationHandoffPayload]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(None))

      val application =
        testApplicationBuilder(Some(userAnswers))
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationReturnRoute)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  // Multi-tab
  "FinalValidationReturnController.onPageLoadWithId" - {

    val flowId        = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val id            = flowId.toString
    val scopedCisId   = "1045"
    val scopedDraftId = "scoped-draft-id"

    val scopedPayload =
      payload.copy(
        instanceId = scopedCisId,
        draftId = scopedDraftId,
        flowId = Some(id)
      )

    val scopedUserAnswers =
      userAnswers
        .set(CisIdPage(id), scopedCisId)
        .success
        .value
        .set(FinalValidationDraftIdPage(id), scopedDraftId)
        .success
        .value

    val finalValidationReturnRouteWithId =
      routes.FinalValidationReturnController
        .onPageLoadWithId(flowId, handoffId)
        .url

    "must delete the handoff and redirect to Update Subcontractor Details with flowId for a valid handoff" in {
      val connector = mock[ConstructionIndustrySchemeConnector]

      when(
        connector.getJourneyHandoff[FinalValidationHandoffPayload](
          any(),
          any[String]
        )(using
          any[Reads[FinalValidationHandoffPayload]],
          any[ClassTag[FinalValidationHandoffPayload]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Some(scopedPayload)))

      doReturn(Future.unit)
        .when(connector)
        .deleteJourneyHandoff(any(), any[String])(using any[HeaderCarrier])

      val application =
        testApplicationBuilder(Some(scopedUserAnswers), multiTabEnabled = true)
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationReturnRouteWithId)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          routes.UpdateSubcontractorDetailsController
            .onPageLoadWithId(flowId, subcontractorId)
            .url

        val handoffIdCaptor = ArgumentCaptor.forClass(classOf[String])

        verify(connector)
          .deleteJourneyHandoff(any(), handoffIdCaptor.capture())(using any[HeaderCarrier])

        handoffIdCaptor.getValue mustBe handoffId
      }
    }

    "must redirect to Journey Recovery when the handoff flowId does not match" in {
      val connector = mock[ConstructionIndustrySchemeConnector]

      when(
        connector.getJourneyHandoff[FinalValidationHandoffPayload](
          any(),
          any[String]
        )(using
          any[Reads[FinalValidationHandoffPayload]],
          any[ClassTag[FinalValidationHandoffPayload]],
          any[HeaderCarrier]
        )
      ).thenReturn(
        Future.successful(
          Some(
            scopedPayload.copy(
              flowId = Some("22222222-2222-2222-2222-222222222222")
            )
          )
        )
      )

      val application =
        testApplicationBuilder(Some(scopedUserAnswers), multiTabEnabled = true)
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationReturnRouteWithId)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(connector)
          .getJourneyHandoff[FinalValidationHandoffPayload](
            any(),
            any[String]
          )(using
            any[Reads[FinalValidationHandoffPayload]],
            any[ClassTag[FinalValidationHandoffPayload]],
            any[HeaderCarrier]
          )

        verify(connector, never())
          .deleteJourneyHandoff(any(), any[String])(using any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery when the handoff does not exist for the scoped journey" in {
      val connector = mock[ConstructionIndustrySchemeConnector]

      when(
        connector.getJourneyHandoff[FinalValidationHandoffPayload](
          any(),
          any[String]
        )(using
          any[Reads[FinalValidationHandoffPayload]],
          any[ClassTag[FinalValidationHandoffPayload]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(None))

      val application =
        testApplicationBuilder(Some(scopedUserAnswers), multiTabEnabled = true)
          .overrides(
            bind[ConstructionIndustrySchemeConnector].toInstance(connector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, finalValidationReturnRouteWithId)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(connector)
          .getJourneyHandoff[FinalValidationHandoffPayload](
            any(),
            any[String]
          )(using
            any[Reads[FinalValidationHandoffPayload]],
            any[ClassTag[FinalValidationHandoffPayload]],
            any[HeaderCarrier]
          )

        verify(connector, never())
          .deleteJourneyHandoff(any(), any[String])(using any[HeaderCarrier])
      }
    }
  }
}
