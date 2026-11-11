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

package views.templates

import base.SpecBase
import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.templates.Layout

class LayoutSpec extends SpecBase {

  "Layout" - {

    "must show the 'Back to agent services' link to the agent dashboard for an agent" in new Setup {
      private val doc = render(viewRequest(FakeRequest(), isAgent = true))

      private val link = doc.select(".govuk-service-navigation a:contains(Back to agent services)")
      link.size mustBe 1
      link.attr("href") mustBe appConfig.hmrcServicesAgentUrl
      link.attr("href") must endWith("/agent/dashboard")
    }

    "must show the 'Back to business tax account' link to the business account for an organisation" in new Setup {
      private val doc = render(viewRequest(FakeRequest(), isAgent = false))

      private val link = doc.select(".govuk-service-navigation a:contains(Back to business tax account)")
      link.size mustBe 1
      link.attr("href") mustBe appConfig.hmrcServicesOrgUrl
      link.attr("href") must endWith("/business-account")
    }

    "must link to the business account when the request has no known user type" in new Setup {
      private val doc = render(FakeRequest())

      doc.select(".govuk-service-navigation a:contains(Back to business tax account)").attr("href") mustBe
        appConfig.hmrcServicesOrgUrl
    }

    "must still show the service name in the service navigation" in new Setup {
      private val doc = render(FakeRequest())

      doc.select(".govuk-service-navigation").text must include(messages("service.name"))
    }

    "must render the organisation link in Welsh when the language is Welsh" in new Setup {
      private val doc = render(FakeRequest(), Lang("cy"))

      doc.select(".govuk-service-navigation a[href$=/business-account]").text mustBe "Yn ôl i’r cyfrif treth busnes"
    }

    "must render the agent link in Welsh when the language is Welsh" in new Setup {
      private val doc = render(viewRequest(FakeRequest(), isAgent = true), Lang("cy"))

      doc.select(".govuk-service-navigation a[href$=/agent/dashboard]").text mustBe "Yn ôl i’r gwasanaethau asiant"
    }
  }

  trait Setup {
    private val app                  = applicationBuilder().build()
    val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
    private val layout: Layout       = app.injector.instanceOf[Layout]
    private val messagesApi          = app.injector.instanceOf[MessagesApi]
    val messages: Messages           = MessagesImpl(Lang.defaultLang, messagesApi)

    def render(request: Request[?], lang: Lang = Lang.defaultLang): Document = {
      given Request[?] = request
      given Messages   = MessagesImpl(lang, messagesApi)
      Jsoup.parse(layout("Test page")(Html("<p>content</p>")).body)
    }
  }
}
