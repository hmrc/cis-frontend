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
      val result = formatter.bind("key", Map("key" -> "value"))
      result mustBe Right("value")
    }

    "must not bind when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind a string with only whitespace" in {
      val result = formatter.bind("key", Map("key" -> "   "))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must bind a string with leading/trailing whitespace" in {
      val result = formatter.bind("key", Map("key" -> "  value  "))
      result mustBe Right("  value  ")
    }

    "must use custom error key with args" in {
      val customFormatter = stringFormatter("custom.error", Seq("arg1", "arg2"))
      val result          = customFormatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "custom.error", Seq("arg1", "arg2"))))
    }

    "must unbind a valid value" in {
      val result = formatter.unbind("key", "value")
      result mustEqual Map("key" -> "value")
    }
  }

  "booleanFormatter" - {

    val formatter = booleanFormatter("error.required", "error.boolean")

    "must bind true" in {
      val result = formatter.bind("key", Map("key" -> "true"))
      result mustBe Right(true)
    }

    "must bind false" in {
      val result = formatter.bind("key", Map("key" -> "false"))
      result mustBe Right(false)
    }

    "must not bind when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an invalid boolean value" in {
      val result = formatter.bind("key", Map("key" -> "maybe"))
      result mustBe Left(Seq(FormError("key", "error.boolean")))
    }

    "must use custom error keys with args" in {
      val customFormatter = booleanFormatter("custom.required", "custom.boolean", Seq("arg1"))
      val result          = customFormatter.bind("key", Map("key" -> "invalid"))
      result mustBe Left(Seq(FormError("key", "custom.boolean", Seq("arg1"))))
    }

    "must unbind true" in {
      val result = formatter.unbind("key", true)
      result mustEqual Map("key" -> "true")
    }

    "must unbind false" in {
      val result = formatter.unbind("key", false)
      result mustEqual Map("key" -> "false")
    }
  }

  "booleanDefaultFalseFormatter" - {

    val formatter = booleanDefaultFalseFormatter("error.required", "error.boolean")

    "must bind true" in {
      val result = formatter.bind("key", Map("key" -> "true"))
      result mustBe Right(true)
    }

    "must bind false when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Right(false)
    }

    "must bind false when value is empty" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Right(false)
    }

    "must bind false when value is invalid" in {
      val result = formatter.bind("key", Map("key" -> "invalid"))
      result mustBe Right(false)
    }

    "must bind false when value is false" in {
      val result = formatter.bind("key", Map("key" -> "false"))
      result mustBe Right(false)
    }

    "must unbind true" in {
      val result = formatter.unbind("key", true)
      result mustEqual Map("key" -> "true")
    }

    "must unbind false" in {
      val result = formatter.unbind("key", false)
      result mustEqual Map("key" -> "false")
    }
  }

  "seqFormatter" - {

    val baseIntFormatter = intFormatter("error.required", "error.wholeNumber", "error.nonNumeric")
    val formatter        = seqFormatter[Int](using baseIntFormatter)

    "must bind a valid sequence" in {
      val data = Map(
        "items.0" -> "1",
        "items.1" -> "2",
        "items.2" -> "3"
      )
      val result = formatter.bind("items", data)
      result mustBe Right(Seq(1, 2, 3))
    }

    "must bind a single item" in {
      val data = Map("items.0" -> "42")
      val result = formatter.bind("items", data)
      result mustBe Right(Seq(42))
    }

    "must bind an empty sequence when no values provided" in {
      val result = formatter.bind("items", Map.empty[String, String])
      result mustBe Right(Seq.empty)
    }

    "must bind items in correct order" in {
      val data = Map(
        "items.2" -> "30",
        "items.0" -> "10",
        "items.1" -> "20"
      )
      val result = formatter.bind("items", data)
      result mustBe Right(Seq(10, 20, 30))
    }

    "must not bind when items have errors" in {
      val data = Map(
        "items.0" -> "1.5",
        "items.1" -> "abc"
      )
      val result = formatter.bind("items", data)
      result.isLeft mustBe true
      result.left.toOption.get.size mustBe 2
    }

    "must ignore keys that don't match the pattern" in {
      val data = Map(
        "items.0" -> "1",
        "other.0" -> "2",
        "items.1" -> "3"
      )
      val result = formatter.bind("items", data)
      result mustBe Right(Seq(1, 3))
    }

    "must unbind a valid sequence" in {
      val result = formatter.unbind("items", Seq(10, 20, 30))
      result mustEqual Map(
        "items.0" -> "10",
        "items.1" -> "20",
        "items.2" -> "30"
      )
    }

    "must unbind an empty sequence" in {
      val result = formatter.unbind("items", Seq.empty)
      result mustEqual Map.empty
    }
  }

  "intFormatter" - {

    val formatter = intFormatter("error.required", "error.wholeNumber", "error.nonNumeric")

    "must bind a valid integer" in {
      val result = formatter.bind("key", Map("key" -> "123"))
      result mustBe Right(123)
    }

    "must bind a negative integer" in {
      val result = formatter.bind("key", Map("key" -> "-456"))
      result mustBe Right(-456)
    }

    "must bind an integer with commas" in {
      val result = formatter.bind("key", Map("key" -> "1,234"))
      result mustBe Right(1234)
    }

    "must not bind when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind a decimal number" in {
      val result = formatter.bind("key", Map("key" -> "1.5"))
      result mustBe Left(Seq(FormError("key", "error.wholeNumber")))
    }

    "must not bind a non-numeric string" in {
      val result = formatter.bind("key", Map("key" -> "abc"))
      result mustBe Left(Seq(FormError("key", "error.nonNumeric")))
    }

    "must use custom error keys with args" in {
      val customFormatter = intFormatter("custom.required", "custom.wholeNumber", "custom.nonNumeric", Seq("arg1"))
      val result          = customFormatter.bind("key", Map("key" -> "1.5"))
      result mustBe Left(Seq(FormError("key", "custom.wholeNumber", Seq("arg1"))))
    }

    "must unbind a valid value" in {
      val result = formatter.unbind("key", 123)
      result mustEqual Map("key" -> "123")
    }
  }

  "enumerableFormatter" - {

    implicit val testEnumEnumerable: Enumerable[TestEnum] = TestEnum.testEnumEnumerable
    val formatter                                         = enumerableFormatter[TestEnum]("error.required", "error.invalid")

    "must bind a valid enum value" in {
      val result = formatter.bind("key", Map("key" -> "Value1"))
      result mustBe Right(Value1)
    }

    "must bind another valid enum value" in {
      val result = formatter.bind("key", Map("key" -> "Value2"))
      result mustBe Right(Value2)
    }

    "must not bind when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an invalid enum value" in {
      val result = formatter.bind("key", Map("key" -> "InvalidValue"))
      result mustBe Left(Seq(FormError("key", "error.invalid")))
    }

    "must use custom error keys with args" in {
      val customFormatter = enumerableFormatter[TestEnum]("custom.required", "custom.invalid", Seq("arg1"))
      val result          = customFormatter.bind("key", Map("key" -> "InvalidValue"))
      result mustBe Left(Seq(FormError("key", "custom.invalid", Seq("arg1"))))
    }

    "must unbind a valid value" in {
      val result = formatter.unbind("key", Value1)
      result mustEqual Map("key" -> "Value1")
    }
  }

  "currencyFormatter" - {

    val formatter = currencyFormatter("error.required", "error.invalidNumeric", "error.nonNumeric")

    "must bind a valid integer" in {
      val result = formatter.bind("key", Map("key" -> "123"))
      result mustBe Right(BigDecimal(123))
    }

    "must bind a valid decimal with 1 decimal place" in {
      val result = formatter.bind("key", Map("key" -> "123.4"))
      result mustBe Right(BigDecimal("123.4"))
    }

    "must bind a valid decimal with 2 decimal places" in {
      val result = formatter.bind("key", Map("key" -> "123.45"))
      result mustBe Right(BigDecimal("123.45"))
    }

    "must bind a number with £ prefix" in {
      val result = formatter.bind("key", Map("key" -> "£123.45"))
      result mustBe Right(BigDecimal("123.45"))
    }

    "must bind a number with commas" in {
      val result = formatter.bind("key", Map("key" -> "1,234.56"))
      result mustBe Right(BigDecimal("1234.56"))
    }

    "must bind a number with spaces" in {
      val result = formatter.bind("key", Map("key" -> "1 234.56"))
      result mustBe Right(BigDecimal("1234.56"))
    }

    "must bind a number with £, commas and spaces" in {
      val result = formatter.bind("key", Map("key" -> "£ 1,234 . 56"))
      result mustBe Right(BigDecimal("1234.56"))
    }

    "must not bind when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind a number with more than 2 decimal places" in {
      val result = formatter.bind("key", Map("key" -> "123.456"))
      result mustBe Left(Seq(FormError("key", "error.invalidNumeric")))
    }

    "must not bind a non-numeric string" in {
      val result = formatter.bind("key", Map("key" -> "abc"))
      result mustBe Left(Seq(FormError("key", "error.nonNumeric")))
    }

    "must not bind a negative number" in {
      val result = formatter.bind("key", Map("key" -> "-123"))
      result mustBe Left(Seq(FormError("key", "error.nonNumeric")))
    }

    "must not bind a number with £ after digits" in {
      val result = formatter.bind("key", Map("key" -> "123£"))
      result mustBe Left(Seq(FormError("key", "error.nonNumeric")))
    }

    "must use custom error keys with args" in {
      val customFormatter = currencyFormatter("custom.required", "custom.invalidNumeric", "custom.nonNumeric", Seq("arg1"))
      val result          = customFormatter.bind("key", Map("key" -> "123.456"))
      result mustBe Left(Seq(FormError("key", "custom.invalidNumeric", Seq("arg1"))))
    }

    "must unbind a valid value" in {
      val result = formatter.unbind("key", BigDecimal("123.45"))
      result mustEqual Map("key" -> "123.45")
    }
  }

  "paymentDetailsCurrencyFormatter" - {

    val formatter = paymentDetailsCurrencyFormatter("error.required", "error.invalid", "error.maxLength")

    "must bind a valid integer" in {
      val result = formatter.bind("key", Map("key" -> "123"))
      result mustBe Right(BigDecimal(123))
    }

    "must bind a valid decimal with 1 decimal place" in {
      val result = formatter.bind("key", Map("key" -> "123.4"))
      result mustBe Right(BigDecimal("123.4"))
    }

    "must bind a valid decimal with 2 decimal places" in {
      val result = formatter.bind("key", Map("key" -> "123.45"))
      result mustBe Right(BigDecimal("123.45"))
    }

    "must bind a number with commas" in {
      val result = formatter.bind("key", Map("key" -> "1,234.56"))
      result mustBe Right(BigDecimal("1234.56"))
    }

    "must bind a number without decimal point" in {
      val result = formatter.bind("key", Map("key" -> "1234"))
      result mustBe Right(BigDecimal(1234))
    }

    "must bind a number with decimal point but no decimal places" in {
      val result = formatter.bind("key", Map("key" -> "1234."))
      result mustBe Right(BigDecimal(1234))
    }

    "must not bind when key is missing" in {
      val result = formatter.bind("key", Map.empty[String, String])
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind an empty string" in {
      val result = formatter.bind("key", Map("key" -> ""))
      result mustBe Left(Seq(FormError("key", "error.required")))
    }

    "must not bind a number with more than 2 decimal places" in {
      val result = formatter.bind("key", Map("key" -> "123.456"))
      result mustBe Left(Seq(FormError("key", "error.invalid")))
    }

    "must not bind a number longer than maxLength" in {
      val result = formatter.bind("key", Map("key" -> "12345678901234"))
      result mustBe Left(Seq(FormError("key", "error.maxLength")))
    }

    "must not bind a number exactly at maxLength" in {
      val result = formatter.bind("key", Map("key" -> "1234567890123"))
      result mustBe Right(BigDecimal("1234567890123"))
    }

    "must not bind a non-numeric string" in {
      val result = formatter.bind("key", Map("key" -> "abc"))
      result mustBe Left(Seq(FormError("key", "error.invalid")))
    }

    "must not bind a number with invalid characters" in {
      val result = formatter.bind("key", Map("key" -> "123.45.67"))
      result mustBe Left(Seq(FormError("key", "error.invalid")))
    }

    "must use custom error keys with args" in {
      val customFormatter = paymentDetailsCurrencyFormatter("custom.required", "custom.invalid", "custom.maxLength", Seq("arg1"))
      val result          = customFormatter.bind("key", Map("key" -> "abc"))
      result mustBe Left(Seq(FormError("key", "custom.invalid", Seq("arg1"))))
    }

    "must unbind a valid value" in {
      val result = formatter.unbind("key", BigDecimal("123.45"))
      result mustEqual Map("key" -> "123.45")
    }
  }
}
