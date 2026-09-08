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

import base.SpecBase

import scala.util.Random

object UTRGenerator:
  val UTR_WEIGHTS: Array[Int] = Array(6, 7, 8, 9, 10, 5, 4, 3, 2)

  private val random: Random = new Random

  def randomUTR(): String =
    val digits      = (1 to 9).map(_ => random.nextInt(10))
    val total       = (0 to 8).map(i => digits(i) * UTR_WEIGHTS(i)).sum
    var checkNumber = 11 - total % 11
    if (checkNumber > 9) checkNumber -= 9
    checkNumber.toString + digits.mkString

class UTRSpec extends SpecBase:
  import UTRGenerator.randomUTR
  import UTR.isValidUTR

  "The UTR utility object" - {

    "returns true for a known valid UTR" in {
      val validUtr = "5860920998"
      isValidUTR(validUtr) mustBe true
    }

    "returns true for a valid random UTR" in {
      val validUtr = randomUTR()
      isValidUTR(validUtr) mustBe true
    }

    "returns true for all of a larger set of random UTR numbers" in {
      val randomUTRs = (1 to 1000).map(_ => randomUTR())
      randomUTRs.foreach { utr =>
        isValidUTR(utr) mustBe true
      }
    }

    "returns false for a UTR of expected length but the check digit is wrong" in {
      val shortUtr = "1234567890"
      isValidUTR(shortUtr) mustBe false
    }

    "returns false for a UTR that is too short" in {
      val shortUtr = "123456789"
      isValidUTR(shortUtr) mustBe false
    }

    "returns false for a UTR that is too long" in {
      val longUtr = "12345678901"
      isValidUTR(longUtr) mustBe false
    }

    "returns false for a UTR with non-numeric characters" in {
      val invalidUtr = "12345A7890"
      isValidUTR(invalidUtr) mustBe false
    }

    "returns false for an empty UTR" in {
      val emptyUtr = ""
      isValidUTR(emptyUtr) mustBe false
    }
  }

end UTRSpec
