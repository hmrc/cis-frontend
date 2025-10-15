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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class IrMarkReferenceGeneratorSpec extends AnyFreeSpec with Matchers {

  "IrMarkReferenceGenerator.fromBase64" - {

    "must convert a known IR mark (base64) to the expected base32 reference without padding" in {
      val base64   = "Pyy1LRJh053AE+nuyp0GJR7oESw="
      val expected = "H4WLKLISMHJZ3QAT5HXMVHIGEUPOQEJM"

      val out = IrMarkReferenceGenerator.fromBase64(base64)
      out mustBe expected
    }

    "must be stable for random bytes and contain only base32 alphabet" in {
      val rnd = new scala.util.Random(42)
      val bytes = Array.fill[Byte](64)((rnd.nextInt(256) - 128).toByte)
      val b64 = java.util.Base64.getEncoder.encodeToString(bytes)

      val out = IrMarkReferenceGenerator.fromBase64(b64)

      out must not be empty
      out.forall(ch => "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".contains(ch)) mustBe true
    }
  }
}


