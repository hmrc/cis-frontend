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

package views.monthlyreturns

import base.SpecBase
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.SubmissionUnsuccessfulView

class SubmissionUnsuccessfulViewSpec extends SpecBase {

  "SubmissionUnsuccessfulView" - {

    "must render the page with correct heading, paragraphs, and other contents" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title                                    must include(messages("monthlyreturns.submissionUnsuccessful.title"))
      doc.select("h1").text                        must include(messages("monthlyreturns.submissionUnsuccessful.heading.h1"))
      doc.select("p").text                         must include(messages("monthlyreturns.submissionUnsuccessful.paragraph.p1"))
      doc.select(".govuk-warning-text__text").text must include(
        messages("monthlyreturns.submissionUnsuccessful.warning")
      )
      doc.select(".govuk-body").text               must include(messages("monthlyreturns.submissionUnsuccessful.links.prefix"))
      doc.select(".govuk-link").text               must include(
        messages("monthlyreturns.submissionUnsuccessful.hmrcOnlineServicesHelpdesk")
      )
      doc.select(".govuk-body").text               must include(messages("monthlyreturns.submissionUnsuccessful.details.intro"))
      doc.select("li").text()                      must include(messages("monthlyreturns.submissionUnsuccessful.details.li1"))
      doc.select("li").text()                      must include(messages("monthlyreturns.submissionUnsuccessful.details.li2"))
      doc.select(".govuk-link").text()             must include(messages("monthlyreturns.submissionUnsuccessful.submit"))
    }

  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[SubmissionUnsuccessfulView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }

}
