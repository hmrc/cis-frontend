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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.finalvalidations.FinalValidationDraftIdPage
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.libs.json.Reads
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

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

  private val finalValidationReturnRoute =
    routes.FinalValidationReturnController.onPageLoad(handoffId).url

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
        applicationBuilder(userAnswers = Some(userAnswers))
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
        applicationBuilder(userAnswers = Some(userAnswers))
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
        applicationBuilder(userAnswers = Some(userAnswers))
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
}
