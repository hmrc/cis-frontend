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

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Content
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText

object insettext extends InsetTextFluency

trait InsetTextFluency {

  object InsetTextViewModel {
    def apply(content: Content): InsetText =
      InsetText(
        content = content
      )
  }

  implicit class FluentInsetText(insetText: InsetText) {

    def withContent(content: Content): InsetText =
      insetText.copy(content = content)

    def withCssClass(newClass: String): InsetText =
      insetText.copy(classes = s"${insetText.classes} $newClass")

    def withAttribute(attribute: (String, String)): InsetText =
      insetText.copy(attributes = insetText.attributes + attribute)
  }
}
