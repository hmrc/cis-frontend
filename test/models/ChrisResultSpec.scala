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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ChrisResultSpec extends AnyFreeSpec with Matchers {

  "Rejected" - {
    "keeps status and body" in {
      val a = ChrisResult.Rejected(400, "bad")
      a.status mustBe 400
      a.body mustBe "bad"
    }
  }

  "UpstreamFailed" - {
    "keeps status and message; equals by value" in {
      val a = ChrisResult.UpstreamFailed(502, "boom")
      a.status mustBe 502
      a.message mustBe "boom"
    }
  }

}
