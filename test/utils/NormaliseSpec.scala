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

package utils

import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class NormaliseSpec extends AnyWordSpec with Matchers {

  "Normalise.nonBlank" should {
    "trim and return value when non-empty" in {
      Normalise.nonBlank(Some("  abc  ")) mustBe Some("abc")
    }

    "return None when blank or empty" in {
      Normalise.nonBlank(Some("   ")) mustBe None
      Normalise.nonBlank(None) mustBe None
    }
  }

  "Normalise.isBLank" should {
    "return true when value is blank or None" in {
      Normalise.isBLank(Some("   ")) mustBe true
      Normalise.isBLank(None) mustBe true
    }

    "return false when value has content" in {
      Normalise.isBLank(Some("abc")) mustBe false
    }
  }

  "Normalise.yesNo" should {
    "convert boolean to yes/no" in {
      Normalise.yesNo(true) mustBe "yes"
      Normalise.yesNo(false) mustBe "no"
    }
  }
}
