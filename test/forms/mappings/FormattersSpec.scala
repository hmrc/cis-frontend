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

package forms.mappings

import models.Enumerable
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError

object FormattersSpec {

  sealed trait TestEnum
  case object Value1 extends TestEnum
  case object Value2 extends TestEnum

  object TestEnum {
    val values: Set[TestEnum] = Set(Value1, Value2)

    implicit val testEnumEnumerable: Enumerable[TestEnum] =
      Enumerable(values.toSeq.map(v => v.toString -> v): _*)
  }
}

class FormattersSpec extends AnyFreeSpec with Matchers with Formatters {

  import FormattersSpec._

  "stringFormatter" - {
    val formatter = stringFormatter("error.required")

    "must bind a valid string" in {
      formatter.bind("key", Map("key" -> "value")) mustBe Right("value")
    }

    "must not bind when key is missing" in {
      formatter.bind("key", Map.empty[String, String]) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      formatter.bind("key", Map("key" -> "")) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind a string with only whitespace" in {
      formatter.bind("key", Map("key" -> "   ")) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must bind a string with leading/trailing whitespace (no trimming performed)" in {
      formatter.bind("key", Map("key" -> "  value  ")) mustBe Right("  value  ")
    }

    "must use custom error key with args" in {
      val customFormatter = stringFormatter("custom.error", Seq("arg1", "arg2"))
      customFormatter.bind("key", Map.empty[String, String]) mustBe
        Left(Seq(FormError("key", "custom.error", Seq("arg1", "arg2"))))
    }

    "must unbind a valid value" in {
      formatter.unbind("key", "value") mustEqual Map("key" -> "value")
    }
  }

  "booleanFormatter" - {
    val formatter = booleanFormatter("error.required", "error.boolean")

    "must bind true" in {
      formatter.bind("key", Map("key" -> "true")) mustBe Right(true)
    }

    "must bind false" in {
      formatter.bind("key", Map("key" -> "false")) mustBe Right(false)
    }

    "must not bind when key is missing" in {
      formatter.bind("key", Map.empty[String, String]) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      formatter.bind("key", Map("key" -> "")) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an invalid boolean value" in {
      formatter.bind("key", Map("key" -> "maybe")) mustBe Left(Seq(FormError("key", "error.boolean")))
    }

    "must use custom error keys with args" in {
      val customFormatter = booleanFormatter("custom.required", "custom.boolean", Seq("arg1"))
      customFormatter.bind("key", Map("key" -> "invalid")) mustBe Left(
        Seq(FormError("key", "custom.boolean", Seq("arg1")))
      )
    }

    "must unbind true" in {
      formatter.unbind("key", true) mustEqual Map("key" -> "true")
    }

    "must unbind false" in {
      formatter.unbind("key", false) mustEqual Map("key" -> "false")
    }
  }

  "booleanDefaultFalseFormatter" - {
    val formatter = booleanDefaultFalseFormatter("error.required", "error.boolean")

    "must bind true" in {
      formatter.bind("key", Map("key" -> "true")) mustBe Right(true)
    }

    "must bind false when key is missing" in {
      formatter.bind("key", Map.empty[String, String]) mustBe Right(false)
    }

    "must bind false when value is empty" in {
      formatter.bind("key", Map("key" -> "")) mustBe Right(false)
    }

    "must bind false when value is invalid" in {
      formatter.bind("key", Map("key" -> "invalid")) mustBe Right(false)
    }

    "must bind false when value is false" in {
      formatter.bind("key", Map("key" -> "false")) mustBe Right(false)
    }

    "must unbind true" in {
      formatter.unbind("key", true) mustEqual Map("key" -> "true")
    }

    "must unbind false" in {
      formatter.unbind("key", false) mustEqual Map("key" -> "false")
    }
  }

  "seqFormatter" - {
    val baseIntFormatter = intFormatter("error.required", "error.wholeNumber", "error.nonNumeric")
    val formatter        = seqFormatter[Int](using baseIntFormatter)

    "must bind a valid sequence" in {
      val data = Map("items.0" -> "1", "items.1" -> "2", "items.2" -> "3")
      formatter.bind("items", data) mustBe Right(Seq(1, 2, 3))
    }

    "must bind a single item" in {
      formatter.bind("items", Map("items.0" -> "42")) mustBe Right(Seq(42))
    }

    "must bind an empty sequence when no values provided" in {
      formatter.bind("items", Map.empty[String, String]) mustBe Right(Seq.empty)
    }

    "must bind items in correct order" in {
      val data = Map("items.2" -> "30", "items.0" -> "10", "items.1" -> "20")
      formatter.bind("items", data) mustBe Right(Seq(10, 20, 30))
    }

    "must not bind when items have errors" in {
      val data   = Map("items.0" -> "1.5", "items.1" -> "abc")
      val result = formatter.bind("items", data)
      result.isLeft mustBe true
      result.left.toOption.get.size mustBe 2
    }

    "must ignore keys that don't match the pattern" in {
      val data = Map("items.0" -> "1", "other.0" -> "2", "items.1" -> "3")
      formatter.bind("items", data) mustBe Right(Seq(1, 3))
    }

    "must unbind a valid sequence" in {
      formatter.unbind("items", Seq(10, 20, 30)) mustEqual Map(
        "items.0" -> "10",
        "items.1" -> "20",
        "items.2" -> "30"
      )
    }

    "must unbind an empty sequence" in {
      formatter.unbind("items", Seq.empty) mustEqual Map.empty
    }
  }

  "intFormatter" - {
    val formatter = intFormatter("error.required", "error.wholeNumber", "error.nonNumeric")

    "must bind a valid integer" in {
      formatter.bind("key", Map("key" -> "123")) mustBe Right(123)
    }

    "must bind a negative integer" in {
      formatter.bind("key", Map("key" -> "-456")) mustBe Right(-456)
    }

    "must bind an integer with commas" in {
      formatter.bind("key", Map("key" -> "1,234")) mustBe Right(1234)
    }

    "must not bind when key is missing" in {
      formatter.bind("key", Map.empty[String, String]) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      formatter.bind("key", Map("key" -> "")) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind a decimal number" in {
      formatter.bind("key", Map("key" -> "1.5")) mustBe Left(Seq(FormError("key", "error.wholeNumber")))
    }

    "must not bind a non-numeric string" in {
      formatter.bind("key", Map("key" -> "abc")) mustBe Left(Seq(FormError("key", "error.nonNumeric")))
    }

    "must use custom error keys with args" in {
      val customFormatter = intFormatter("custom.required", "custom.wholeNumber", "custom.nonNumeric", Seq("arg1"))
      customFormatter.bind("key", Map("key" -> "1.5")) mustBe Left(
        Seq(FormError("key", "custom.wholeNumber", Seq("arg1")))
      )
    }

    "must unbind a valid value" in {
      formatter.unbind("key", 123) mustEqual Map("key" -> "123")
    }
  }

  "enumerableFormatter" - {
    implicit val testEnumEnumerable: Enumerable[TestEnum] = TestEnum.testEnumEnumerable
    val formatter                                         = enumerableFormatter[TestEnum]("error.required", "error.invalid")

    "must bind a valid enum value" in {
      formatter.bind("key", Map("key" -> "Value1")) mustBe Right(Value1)
    }

    "must bind another valid enum value" in {
      formatter.bind("key", Map("key" -> "Value2")) mustBe Right(Value2)
    }

    "must not bind when key is missing" in {
      formatter.bind("key", Map.empty[String, String]) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      formatter.bind("key", Map("key" -> "")) mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an invalid enum value" in {
      formatter.bind("key", Map("key" -> "InvalidValue")) mustBe Left(Seq(FormError("key", "error.invalid")))
    }

    "must use custom error keys with args" in {
      val customFormatter = enumerableFormatter[TestEnum]("custom.required", "custom.invalid", Seq("arg1"))
      customFormatter.bind("key", Map("key" -> "InvalidValue")) mustBe Left(
        Seq(FormError("key", "custom.invalid", Seq("arg1")))
      )
    }

    "must unbind a valid value" in {
      formatter.unbind("key", Value1) mustEqual Map("key" -> "Value1")
    }
  }

  "currencyFormatter" - {

    "scale=0 (whole pounds)" - {
      val formatter = currencyFormatter(
        requiredKey = "required",
        invalidKey = "invalid",
        maxLengthKey = "maxLength",
        scale = 0
      )

      "must bind whole numbers" in {
        Seq("0", "100", "99999999", "1,000", "£100", " 1 0 0 ", "100.", "100.0", "100.00").foreach { in =>
          withClue(s"input='$in'") {
            formatter.bind("key", Map("key" -> in)).isRight mustBe true
          }
        }
      }

      "must reject non-zero decimals" in {
        Seq("0.01", "100.5", "100.50", "1,234.56").foreach { in =>
          withClue(s"input='$in'") {
            formatter.bind("key", Map("key" -> in)) mustBe Left(Seq(FormError("key", "invalid")))
          }
        }
      }

      "must reject negatives and invalid patterns" in {
        Seq("-1", "abc", "100£", "1..0", "1.0.0").foreach { in =>
          withClue(s"input='$in'") {
            formatter.bind("key", Map("key" -> in)) mustBe Left(Seq(FormError("key", "invalid")))
          }
        }
      }

      "must enforce max length" in {
        val tooLong = "1" * 17
        formatter.bind("key", Map("key" -> tooLong)) mustBe Left(Seq(FormError("key", "maxLength")))
      }

      "must unbind as whole pounds with comma grouping and no decimals" in {
        formatter.unbind("key", BigDecimal("12345.00")) mustBe Map("key" -> "12,345")
      }

      "must unbind without commas when not needed" in {
        formatter.unbind("key", BigDecimal("123.00")) mustBe Map("key" -> "123")
      }

    }

    "scale=2 (pounds and pence)" - {
      val formatter = currencyFormatter(
        requiredKey = "required",
        invalidKey = "invalid",
        maxLengthKey = "maxLength",
        scale = 2
      )

      "must bind up to 2dp" in {
        Seq("0", "0.0", "0.00", "1.2", "1.23", "£1,234.56", " 1 2 3 4 . 5 6 ").foreach { in =>
          withClue(s"input='$in'") {
            formatter.bind("key", Map("key" -> in)).isRight mustBe true
          }
        }
      }

      "must reject more than 2dp and invalid patterns" in {
        Seq("1.234", "1.2.3", "abc", "-1").foreach { in =>
          withClue(s"input='$in'") {
            formatter.bind("key", Map("key" -> in)) mustBe Left(Seq(FormError("key", "invalid")))
          }
        }
      }

      "must unbind with exactly 2dp and comma grouping" in {
        formatter.unbind("key", BigDecimal("12345")) mustBe Map("key" -> "12,345.00")
      }
    }
  }
}
