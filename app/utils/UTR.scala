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

import org.apache.commons.lang3.StringUtils

object UTR:
  private val UTR_WEIGHTS = Array(6, 7, 8, 9, 10, 5, 4, 3, 2)
  private val utrPattern  = "^[0-9]{10}$".r

  def isValidUTR(utr: String): Boolean = {
    if (utr == null || StringUtils.isBlank(utr)) return false
    utr match {
      case utrPattern() =>
        val (leadDigit, rest) = (utr.head.asDigit, utr.tail)
        val sum               = rest.zipWithIndex.map((d, i) => d.asDigit * UTR_WEIGHTS(i)).sum
        val remainder         = sum % 11
        var checkNumber       = 11 - remainder
        if (checkNumber > 9) checkNumber -= 9
        leadDigit == checkNumber
      case _            => false
    }
  }

end UTR
