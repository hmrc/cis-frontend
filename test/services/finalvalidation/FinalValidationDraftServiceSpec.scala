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

package services.finalvalidation

import base.SpecBase
import connectors.ConstructionIndustrySchemeConnector
import models.finalvalidation.*
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import services.finalvalidation.FinalValidationDraftService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class FinalValidationDraftServiceSpec extends SpecBase {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  private val connector = mock[ConstructionIndustrySchemeConnector]
  private val service   = new FinalValidationDraftService(connector)

  "get" - {

    "must return the draft from the connector" in {
      val draft = mock[FinalValidationDraft]

      when(
        connector.getFinalValidationDraft(
          "instance-id",
          "draft-id"
        )
      ).thenReturn(Future.successful(draft))

      service
        .get("instance-id", "draft-id")
        .futureValue mustBe draft

      verify(connector).getFinalValidationDraft(
        "instance-id",
        "draft-id"
      )
    }
  }

  "updateReadiness" - {

    "must update readiness using the connector" in {
      val draft  = mock[FinalValidationDraft]
      val issues = Seq.empty[FinalValidationDraftIssue]

      when(
        connector.updateFinalValidationReadiness(
          "instance-id",
          "draft-id",
          UpdateFinalValidationReadinessRequest(
            subcontractorId = 101L,
            issues = issues
          )
        )
      ).thenReturn(Future.successful(draft))

      service
        .updateReadiness(
          instanceId = "instance-id",
          draftId = "draft-id",
          subcontractorId = 101L,
          issues = issues
        )
        .futureValue mustBe draft

      verify(connector).updateFinalValidationReadiness(
        "instance-id",
        "draft-id",
        UpdateFinalValidationReadinessRequest(
          subcontractorId = 101L,
          issues = issues
        )
      )
    }
  }

  "commit" - {

    "must commit the draft using the connector" in {
      when(
        connector.commitFinalValidationDraft(
          "instance-id",
          "draft-id"
        )
      ).thenReturn(Future.successful(()))

      service
        .commit("instance-id", "draft-id")
        .futureValue mustBe ()

      verify(connector).commitFinalValidationDraft(
        "instance-id",
        "draft-id"
      )
    }
  }
}
