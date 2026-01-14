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

package views.components

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

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers.*

class CaptionSpec extends SpecBase {

  private val caption = app.injector.instanceOf[views.html.components.Caption]

  "Caption" - {

    "render with default class and given text" in {
      val html = caption("ABC Construction Ltd")
      val doc  = Jsoup.parse(html.body)

      val span = doc.selectFirst("span")
      span               should not be null
      span.className() shouldBe "govuk-caption-l"
      span.text()      shouldBe "ABC Construction Ltd"
    }

    "allow overriding the CSS classes" in {
      val html = caption("Some text", "govuk-caption-m extra-class")
      val doc  = Jsoup.parse(html.body)

      val span = doc.selectFirst("span")
      span.className() shouldBe "govuk-caption-m extra-class"
      span.text()      shouldBe "Some text"
    }
  }
}
