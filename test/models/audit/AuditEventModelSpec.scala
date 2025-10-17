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

package models.audit

import base.SpecBase
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.libs.json.{JsValue, Json}

class AuditEventModelSpec extends SpecBase {

  "AuthFailureAuditEventModel" - {
    val underTest = AuthFailureAuditEventModel()
    "must serialise correctly" in {
      Json.toJson(underTest) mustBe Json.obj()
    }
  }

  "extendedDataEvent" - {
    val testAuditType: String   = "test-audit-type"
    val testDetailJson: JsValue = Json.toJson(testAuditType)
    "behave as expected" in {
      val event    = new AuditEventModel {
        override val auditType: String   = testAuditType
        override val detailJson: JsValue = testDetailJson
      }
      val extended = event.extendedDataEvent
      extended.auditType shouldBe testAuditType
      extended.detail    shouldBe testDetailJson
    }
  }

}
