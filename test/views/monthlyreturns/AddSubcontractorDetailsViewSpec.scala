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
import forms.monthlyreturns.AddSubcontractorDetailsFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.AddSubcontractorDetailsView

class AddSubcontractorDetailsViewSpec extends SpecBase {

  "AddSubcontractorDetailsView" - {

    "must render the page with the correct title, heading, bullet list and button" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title             must include(messages("monthlyreturns.addSubcontractorDetails.title"))
      doc.select("h1").text must include(messages("monthlyreturns.addSubcontractorDetails.heading"))

      doc.getElementsByClass("govuk-body").text() must include(
        messages("monthlyreturns.addSubcontractorDetails.alreadyAdded")
      )

      val listText = doc.getElementsByClass("govuk-list").text()
      listText must include("BuildRight Construction")

      doc.getElementsByClass("govuk-button").text must include(messages("site.continue"))
    }

    "must render radio buttons with the correct values and labels" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[type=radio][value=option1]").size() mustBe 1
      doc.select("input[type=radio][value=option2]").size() mustBe 1

      doc.select("label[for=option1]").text() must include("Northern Trades Ltd")
      doc.select("label[for=option2]").text() must include("TyneWear Ltd")
    }
  }

  trait Setup {
    private val app                               = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[AddSubcontractorDetailsView]
    private val formProvider                      = new AddSubcontractorDetailsFormProvider()
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    private val subcontractorsWithDetails: Seq[String] =
      Seq("BuildRight Construction")

    private val subcontractorsWithoutDetails: Seq[String] =
      Seq("Northern Trades Ltd", "TyneWear Ltd")

    val html = view(form, NormalMode, subcontractorsWithDetails, subcontractorsWithoutDetails)
  }
}
