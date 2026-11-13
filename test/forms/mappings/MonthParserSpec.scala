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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MonthParserSpec extends AnyWordSpec with Matchers {

  private val englishFull  =
    Seq(
      "January",
      "February",
      "March",
      "April",
      "May",
      "June",
      "July",
      "August",
      "September",
      "October",
      "November",
      "December"
    )
  private val welshFull    =
    Seq(
      "Ionawr",
      "Chwefror",
      "Mawrth",
      "Ebrill",
      "Mai",
      "Mehefin",
      "Gorffennaf",
      "Awst",
      "Medi",
      "Hydref",
      "Tachwedd",
      "Rhagfyr"
    )
  private val welshAbbrevs =
    Seq("Ion", "Chwe", "Maw", "Ebr", "Mai", "Meh", "Gorff", "Awst", "Medi", "Hyd", "Tach", "Rhag")

  "MonthParser.parse" should {

    "parse full English month names" in {
      englishFull.zipWithIndex.foreach { case (name, i) =>
        MonthParser.parse(name, isWelsh = false) mustBe Some(i + 1)
      }
    }

    "parse 3 letter English abbreviations" in {
      englishFull.zipWithIndex.foreach { case (name, i) =>
        MonthParser.parse(name.take(3), isWelsh = false) mustBe Some(i + 1)
      }
    }

    "be case insensitive" in {
      MonthParser.parse("nOvEmBeR", isWelsh = false) mustBe Some(11)
      MonthParser.parse("JAN", isWelsh = false) mustBe Some(1)
    }

    "accept Sept and a trailing full stop" in {
      MonthParser.parse("Sept", isWelsh = false) mustBe Some(9)
      MonthParser.parse("Sept.", isWelsh = false) mustBe Some(9)
      MonthParser.parse("Nov.", isWelsh = false) mustBe Some(11)
    }

    "ignore all whitespace" in {
      MonthParser.parse("  N o v    ", isWelsh = false) mustBe Some(11)
      MonthParser.parse("Nov ", isWelsh = false) mustBe Some(11)
      MonthParser.parse("Jan\t", isWelsh = false) mustBe Some(1)
    }

    "parse full Welsh month names when Welsh" in {
      welshFull.zipWithIndex.foreach { case (name, i) =>
        MonthParser.parse(name, isWelsh = true) mustBe Some(i + 1)
      }
    }

    "parse Welsh abbreviations when Welsh" in {
      welshAbbrevs.zipWithIndex.foreach { case (name, i) =>
        MonthParser.parse(name, isWelsh = true) mustBe Some(i + 1)
      }
    }

    "still accept English names when Welsh" in {
      MonthParser.parse("November", isWelsh = true) mustBe Some(11)
    }

    "not accept Welsh names when not Welsh" in {
      MonthParser.parse("Tachwedd", isWelsh = false) mustBe None
      MonthParser.parse("Ionawr", isWelsh = false) mustBe None
    }

    "not accept numeric months" in {
      Seq("1", "01", "11", "12", "0", "13", " 1 ").foreach { n =>
        MonthParser.parse(n, isWelsh = false) mustBe None
        MonthParser.parse(n, isWelsh = true) mustBe None
      }
    }

    "reject empty, blank and unrecognised input" in {
      Seq("", "   ", "Foo", "Janu", "Novembr", "Nove", "Jan1").foreach { v =>
        MonthParser.parse(v, isWelsh = false) mustBe None
      }
    }
  }

  "MonthParser.displayName" should {

    "return full English names" in {
      (1 to 12).foreach(m => MonthParser.displayName(m, isWelsh = false) mustBe englishFull(m - 1))
    }

    "return capitalised Welsh names" in {
      (1 to 12).foreach(m => MonthParser.displayName(m, isWelsh = true) mustBe welshFull(m - 1))
    }

    "round trip through parse" in {
      (1 to 12).foreach { m =>
        MonthParser.parse(MonthParser.displayName(m, isWelsh = false), isWelsh = false) mustBe Some(m)
        MonthParser.parse(MonthParser.displayName(m, isWelsh = true), isWelsh = true) mustBe Some(m)
      }
    }
  }
}
