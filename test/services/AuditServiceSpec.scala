/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import base.SpecBase
import models.audit.AuthFailureAuditEventModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Headers, Request}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends SpecBase with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  private val mockAuditConnector                    = mock[AuditConnector]
  private val service: AuditService                 = app.injector.instanceOf[AuditService]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val testUri                               = "testUri"

  implicit val ec: ExecutionContext = global

  ".sendEvent" - {
    "create extended event and send to auditConnector" in {
      implicit val request: Request[?] = FakeRequest("GET", testUri, Headers(), "")

      val auditEvent = AuthFailureAuditEventModel()

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = service.sendEvent(auditEvent).futureValue

      result mustBe AuditResult.Success

      auditEvent.auditType mustBe "authoriseServiceGuardFailure"
    }
  }
}
