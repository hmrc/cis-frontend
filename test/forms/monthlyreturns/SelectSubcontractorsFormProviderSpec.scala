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

package forms.monthlyreturns

import forms.behaviours.FieldBehaviours
import models.monthlyreturns.SelectSubcontractorsFormData
import play.api.data.{Form, FormError}

class SelectSubcontractorsFormProviderSpec extends FieldBehaviours {
  val form: Form[SelectSubcontractorsFormData] = SelectSubcontractorsFormProvider()()

  ".confirmation" - {

    val fieldName = "confirmation"

    "bind true" in {
      val data   = Map(fieldName -> "true")
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value.confirmation mustBe true
    }

    "bind false" in {
      val data   = Map(fieldName -> "false")
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value.confirmation mustBe false
    }

    "default to false for non-booleans" in {
      val data   = Map(fieldName -> "invalid")
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value.confirmation mustBe false
    }

    "default to false when key is not present at all" in {
      val result = form.bind(Map.empty[String, String])
      result.errors mustBe empty
      result.value.value.confirmation mustBe false
    }

    "default to false for blank values" in {
      val data   = Map(fieldName -> "")
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value.confirmation mustBe false
    }
  }

  ".subcontractorsToInclude" - {

    "bind valid integer sequences" in {
      val data   = Map(
        "confirmation"              -> "true",
        "subcontractorsToInclude.0" -> "1",
        "subcontractorsToInclude.1" -> "2",
        "subcontractorsToInclude.2" -> "3"
      )
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value mustBe SelectSubcontractorsFormData(
        confirmation = true,
        subcontractorsToInclude = Seq(1, 2, 3)
      )
    }

    "bind single integer value" in {
      val data   = Map(
        "confirmation"              -> "false",
        "subcontractorsToInclude.0" -> "42"
      )
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value mustBe SelectSubcontractorsFormData(
        confirmation = false,
        subcontractorsToInclude = Seq(42)
      )
    }

    "bind empty sequence when no subcontractors provided" in {
      val data   = Map(
        "confirmation" -> "true"
      )
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value mustBe SelectSubcontractorsFormData(
        confirmation = true,
        subcontractorsToInclude = Seq.empty
      )
    }

    "fail to bind when non-numeric values are provided" in {
      val data   = Map(
        "confirmation"              -> "true",
        "subcontractorsToInclude.0" -> "abc"
      )
      val result = form.bind(data)
      result.errors must contain(
        FormError(
          "subcontractorsToInclude.0",
          "monthlyreturns.selectSubcontractors.subcontractorsToInclude.nonNumeric"
        )
      )
    }

    "fail to bind when decimal values are provided" in {
      val data   = Map(
        "confirmation"              -> "true",
        "subcontractorsToInclude.0" -> "1.5"
      )
      val result = form.bind(data)
      result.errors must contain(
        FormError(
          "subcontractorsToInclude.0",
          "monthlyreturns.selectSubcontractors.subcontractorsToInclude.wholeNumber"
        )
      )
    }

    "handle multiple errors in sequence" in {
      val data   = Map(
        "confirmation"              -> "true",
        "subcontractorsToInclude.0" -> "1.5",
        "subcontractorsToInclude.1" -> "abc"
      )
      val result = form.bind(data)
      result.errors.length mustBe 2
      result.errors must contain(
        FormError(
          "subcontractorsToInclude.0",
          "monthlyreturns.selectSubcontractors.subcontractorsToInclude.wholeNumber"
        )
      )
      result.errors must contain(
        FormError(
          "subcontractorsToInclude.1",
          "monthlyreturns.selectSubcontractors.subcontractorsToInclude.nonNumeric"
        )
      )
    }
  }

  "form" - {

    "bind successfully with all valid data" in {
      val data   = Map(
        "confirmation"              -> "true",
        "subcontractorsToInclude.0" -> "10",
        "subcontractorsToInclude.1" -> "20",
        "subcontractorsToInclude.2" -> "30"
      )
      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value mustBe SelectSubcontractorsFormData(
        confirmation = true,
        subcontractorsToInclude = Seq(10, 20, 30)
      )
    }

    "unbind successfully" in {
      val formData = SelectSubcontractorsFormData(
        confirmation = true,
        subcontractorsToInclude = Seq(5, 10, 15)
      )
      val result   = form.fill(formData)
      result.data mustBe Map(
        "confirmation"              -> "true",
        "subcontractorsToInclude.0" -> "5",
        "subcontractorsToInclude.1" -> "10",
        "subcontractorsToInclude.2" -> "15"
      )
    }
  }
}
