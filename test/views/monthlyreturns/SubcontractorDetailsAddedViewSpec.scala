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

import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{Call, Request}
import play.api.routing.Router
import play.api.test.FakeRequest
import forms.monthlyreturns.SubcontractorDetailsAddedFormProvider
import views.html.monthlyreturns.SubcontractorDetailsAddedView

import viewmodels.checkAnswers.monthlyreturns._

class SubcontractorDetailsAddedViewSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  "SubcontractorDetailsAddedView" - {

    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title() must include(messages("monthlyreturns.subcontractorDetailsAdded.heading"))

      doc.select("h1").text()                       must include(messages("monthlyreturns.subcontractorDetailsAdded.heading"))
      doc.getElementsByClass("govuk-button").text() must include(messages("site.continue"))

      doc.text() must include(messages("monthlyreturns.subcontractorDetailsAdded.question"))
      doc.text() must include(messages("monthlyreturns.subcontractorDetailsAdded.hint"))
    }

    "must display error summary when form has errors" in new Setup {
      val formWithError = form.withError("value", "monthlyreturns.subcontractorDetailsAdded.error.incomplete")
      val htmlWithError = view(formWithError, NormalMode, viewModel)
      val doc: Document = Jsoup.parse(htmlWithError.toString)

      doc.select(".govuk-error-summary").size() mustBe 1
    }
  }

  trait Setup {
    val app = new GuiceApplicationBuilder()
      .configure(
        "host"                          -> "http://localhost:9000",
        "timeout-dialog.timeout"        -> 900,
        "timeout-dialog.countdown"      -> 120,
        "timeout-dialog.keep-alive-url" -> "/keep-alive",
        "timeout-dialog.sign-out-url"   -> "/sign-out",
        "contact-frontend.serviceId"    -> "cis-frontend",
        "contact-frontend.host"         -> "http://localhost:9250",
        "features.welsh-translation"    -> false
      )
      .overrides(
        bind[Router].toInstance(Router.empty)
      )
      .build()

    val view: SubcontractorDetailsAddedView = app.injector.instanceOf[SubcontractorDetailsAddedView]
    val formProvider                        = new SubcontractorDetailsAddedFormProvider()
    val form                                = formProvider()

    implicit val request: Request[_] = FakeRequest()
    implicit val messages: Messages  = MessagesImpl(
      Lang.defaultLang,
      app.injector.instanceOf[MessagesApi]
    )

    val viewModel: SubcontractorDetailsAddedViewModel =
      SubcontractorDetailsAddedViewModel(
        headingKey = "monthlyreturns.subcontractorDetailsAdded.heading",
        headingArgs = Seq.empty,
        rows = Seq(
          SubcontractorDetailsAddedRow(
            index = 0,
            subcontractorId = 1001L,
            name = "TyneWear Ltd",
            detailsAdded = true,
            changeCall = Call("GET", "/change-1"),
            removeCall = Call("GET", "/remove-1")
          ),
          SubcontractorDetailsAddedRow(
            index = 1,
            subcontractorId = 1002L,
            name = "Northern Trades Ltd",
            detailsAdded = false,
            changeCall = Call("GET", "/change-2"),
            removeCall = Call("GET", "/remove-2")
          )
        ),
        hasIncomplete = true
      )

    val html = view(form, NormalMode, viewModel)
  }
}
