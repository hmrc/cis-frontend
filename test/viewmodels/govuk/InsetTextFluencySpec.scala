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

package viewmodels.govuk

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.govuk.all._

class InsetTextFluencySpec extends AnyFreeSpec with Matchers {

  "InsetTextViewModel" - {

    "apply" - {
      "must create an InsetText with the given content" in {
        val content = Text("Test content")
        val result = insettext.InsetTextViewModel(content)
        
        result.content mustEqual content
      }
    }
  }

  "FluentInsetText" - {

    "withContent" - {
      "must update the content" in {
        val originalContent = Text("Original content")
        val newContent = Text("New content")
        val insetText = insettext.InsetTextViewModel(originalContent)
        
        val result = insetText.withContent(newContent)
        
        result.content mustEqual newContent
      }
    }

    "withCssClass" - {
      "must add the new class to existing classes" in {
        val insetText = insettext.InsetTextViewModel(Text("Test"))
        val result = insetText.withCssClass("test-class")
        
        result.classes must include("test-class")
      }
    }

    "withAttribute" - {
      "must add the new attribute" in {
        val insetText = insettext.InsetTextViewModel(Text("Test"))
        val result = insetText.withAttribute("data-test" -> "value")
        
        result.attributes must contain("data-test" -> "value")
      }
    }
  }
}
