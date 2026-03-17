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

package models

import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsString, Json}
import play.api.mvc.QueryStringBindable

class ReturnTypeSpec extends AnyFreeSpec with Matchers with OptionValues with EitherValues with Enumerable.Implicits {

  private val binder   = implicitly[QueryStringBindable[ReturnType]]
  private val paramKey = "returnType"

  private def params(value: String): Map[String, Seq[String]] = Map(paramKey -> Seq(value))

  ".values" - {

    "must contain all return types" in {
      ReturnType.values mustEqual Seq(MonthlyNilReturn, MonthlyStandardReturn)
    }
  }

  ".toString" - {

    "MonthlyNilReturn must have the correct name" in {
      MonthlyNilReturn.toString mustEqual "monthlyNilReturn"
    }

    "MonthlyStandardReturn must have the correct name" in {
      MonthlyStandardReturn.toString mustEqual "monthlyStandardReturn"
    }
  }

  ".enumerable (JSON)" - {

    "must read MonthlyNilReturn from JSON" in {
      Json.fromJson[ReturnType](JsString("monthlyNilReturn")).asEither.value mustEqual MonthlyNilReturn
    }

    "must read MonthlyStandardReturn from JSON" in {
      Json.fromJson[ReturnType](JsString("monthlyStandardReturn")).asEither.value mustEqual MonthlyStandardReturn
    }

    "must fail to read an unrecognised value from JSON" in {
      Json.fromJson[ReturnType](JsString("invalid")).isError mustBe true
    }

    "must write MonthlyNilReturn to JSON" in {
      Json.toJson[ReturnType](MonthlyNilReturn) mustEqual JsString("monthlyNilReturn")
    }

    "must write MonthlyStandardReturn to JSON" in {
      Json.toJson[ReturnType](MonthlyStandardReturn) mustEqual JsString("monthlyStandardReturn")
    }
  }

  ".jsLiteral" - {

    "must produce MonthlyNilReturn as a JavaScript string" in {
      ReturnType.jsLiteral.to(MonthlyNilReturn) mustEqual "MonthlyNilReturn"
    }

    "must produce MonthlyStandardReturn as a JavaScript string" in {
      ReturnType.jsLiteral.to(MonthlyStandardReturn) mustEqual "MonthlyStandardReturn"
    }
  }

  ".queryStringBindable" - {

    "bind" - {

      "must bind MonthlyNilReturn from a query string" in {
        binder.bind(paramKey, params("MonthlyNilReturn")).value.value mustEqual MonthlyNilReturn
      }

      "must bind MonthlyStandardReturn from a query string" in {
        binder.bind(paramKey, params("MonthlyStandardReturn")).value.value mustEqual MonthlyStandardReturn
      }

      "must return Left for an unrecognised value" in {
        binder.bind(paramKey, params("Invalid")).value.left.value mustEqual "Unknown ReturnType: Invalid"
      }

      "must return None when the key is absent" in {
        binder.bind(paramKey, Map.empty) mustBe None
      }
    }

    "unbind" - {

      "must unbind MonthlyNilReturn to the correct query string" in {
        binder.unbind(paramKey, MonthlyNilReturn) mustEqual s"$paramKey=MonthlyNilReturn"
      }

      "must unbind MonthlyStandardReturn to the correct query string" in {
        binder.unbind(paramKey, MonthlyStandardReturn) mustEqual s"$paramKey=MonthlyStandardReturn"
      }
    }
  }
}
