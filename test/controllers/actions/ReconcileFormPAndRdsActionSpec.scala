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
import models.monthlyreturns.CisTaxpayer
import models.requests.{IdentifierRequest, SchemeRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{NOT_FOUND, OK, PRECONDITION_FAILED, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.api.mvc.Results
import services.FormpRdsReconcileService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class ReconcileFormPAndRdsActionSpec extends SpecBase {

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private def schemeRequest = {
    val identifier = IdentifierRequest(FakeRequest(GET, "/monthly-return"), userAnswersId, None, None)
    val taxpayer   = mock(classOf[CisTaxpayer])

    when(taxpayer.uniqueId).thenReturn("123456789")
    when(taxpayer.taxOfficeNumber).thenReturn("123")
    when(taxpayer.taxOfficeRef).thenReturn("AB456")

    new SchemeRequest(identifier, taxpayer)
  }

  private def actionWith(result: Future[Unit]): ReconcileFormPAndRdsActionImpl = {
    val service = mock(classOf[FormpRdsReconcileService])

    when(
      service.reconcile(any[String], any[String], any[String])(using any[HeaderCarrier])
    ).thenReturn(result)

    new ReconcileFormPAndRdsActionImpl(service)
  }

  "ReconcileFormPAndRdsAction" - {

    "continue when reconciliation succeeds" in {
      val action = actionWith(Future.unit)

      val result = action
        .invokeBlock(
          schemeRequest,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe OK
    }

    Seq(PRECONDITION_FAILED, NOT_FOUND).foreach { status =>
      s"redirect to unauthorised organisation affinity when reconciliation returns $status" in {
        val action = actionWith(
          Future.failed(UpstreamErrorResponse("Known facts missing", status, status))
        )

        val result = action
          .invokeBlock(
            schemeRequest,
            _ => Future.successful(Results.Ok)
          )
          .futureValue

        result.header.status mustBe SEE_OTHER
        result.header.headers.get("Location") mustBe Some(
          controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
        )
      }
    }

    "redirect to journey recovery when reconciliation fails unexpectedly" in {
      val action = actionWith(Future.failed(new RuntimeException("Backend error")))

      val result = action
        .invokeBlock(
          schemeRequest,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      )
    }
  }
}
