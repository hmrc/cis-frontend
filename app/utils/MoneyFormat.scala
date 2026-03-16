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

import java.text.DecimalFormat
import java.math.RoundingMode

object MoneyFormat {

  private val df: DecimalFormat = {
    val f = new DecimalFormat("#,##0.00")
    f.setRoundingMode(RoundingMode.HALF_UP)
    f
  }

  /** Always renders with exactly 2dp, no commas, no currency symbol. */
  def twoDp(value: BigDecimal): String =
    df.format(value.bigDecimal)

  /** Convenience for Option[BigDecimal]. */
  def twoDpOrEmpty(value: Option[BigDecimal]): String =
    value.map(twoDp).getOrElse("")
}
