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

package utils

import base.SpecBase

class ReferenceGeneratorSpec extends SpecBase {

  val referenceGeneratorImpl = new ReferenceGeneratorImpl()

  "ReferenceGenerator" - {

    "should generate a 16-character alphanumeric string and not be empty" in {
      val referenceNumber = referenceGeneratorImpl.generateReference()

      referenceNumber.length mustBe 16
      referenceNumber must fullyMatch regex "^[A-Za-z0-9]+$"
      referenceNumber must not be empty
    }
  }
}
