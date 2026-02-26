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

package views.components

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.components.LoadingSpinner

class LoadingSpinnerSpec extends SpecBase with Matchers {

  "LoadingSpinner" - {

    "must render the page with correct heading, paragraphs, and other contents" in new Setup {
      val html = loadingSpinner("h1text", "paragraph text")
      val doc  = Jsoup.parse(html.body)

      doc.getElementsByClass("loading-spinner").size mustBe 1
      doc.getElementsByClass("loading-spinner__spinner").size mustBe 1
      doc.getElementsByClass("loading-spinner__content").size mustBe 1
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val loadingSpinner: LoadingSpinner            = app.injector.instanceOf[LoadingSpinner]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
