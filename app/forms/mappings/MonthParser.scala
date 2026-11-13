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

package forms.mappings

import java.time.Month
import java.util.Locale

object MonthParser {

  private val welshMonths: Seq[String] =
    Seq(
      "ionawr",
      "chwefror",
      "mawrth",
      "ebrill",
      "mai",
      "mehefin",
      "gorffennaf",
      "awst",
      "medi",
      "hydref",
      "tachwedd",
      "rhagfyr"
    )

  private val welshShort: Seq[Seq[String]] =
    Seq(
      Seq("ion"),
      Seq("chwe"),
      Seq("maw"),
      Seq("ebr"),
      Seq("mai"),
      Seq("meh"),
      Seq("gorff"),
      Seq("awst"),
      Seq("medi"),
      Seq("hyd"),
      Seq("tach"),
      Seq("rhag")
    )

  private val englishMonths: Seq[String] = Month.values.toSeq.map(_.toString.toLowerCase(Locale.UK))

  /** Parses a month entered as an English name/abbreviation (e.g. Jan, January, Sept) and, when isWelsh, a Welsh
    * name/abbreviation (e.g. Ion, Ionawr).
    */
  def parse(input: String, isWelsh: Boolean): Option[Int] = {
    val value = input.replaceAll("[\\s\\u00A0]+", "").toLowerCase(Locale.UK).stripSuffix(".")

    if (value.isEmpty) None
    else {
      val english = englishMonths.indexWhere(m =>
        m == value || (value.length == 3 && m.take(3) == value) || (value == "sept" && m == "september")
      )
      val welsh   =
        if (isWelsh)
          welshMonths.indices.find(i => welshMonths(i) == value || welshShort(i).contains(value)).getOrElse(-1)
        else -1
      Seq(english, welsh).find(_ >= 0).map(_ + 1)
    }
  }

  /** Full month name (1-12) for redisplaying a bound value in the user's language. */
  def displayName(month: Int, isWelsh: Boolean): String =
    if (isWelsh) welshMonths(month - 1).capitalize
    else Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.UK)
}
