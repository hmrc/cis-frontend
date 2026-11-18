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

package views.monthlyreturns

import base.SpecBase
import config.FrontendAppConfig
import forms.monthlyreturns.DateConfirmPaymentsFormProvider
import models.NormalMode
import models.ReturnType.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.DateConfirmPaymentsView

import java.time.{Clock, Instant, ZoneId}

class DateConfirmPaymentsViewSpec extends SpecBase with MockitoSugar {

  "DateConfirmPaymentsView" - {

    "must render the month input with a text keyboard and the day and year inputs as numeric" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[name$=month]").attr("inputmode") mustBe "text"
      doc.select("input[name$=year]").attr("inputmode") mustBe "numeric"
    }

    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title             must include(messages("monthlyreturns.dateConfirmPayments.title"))
      doc.select("h1").text must include(messages("monthlyreturns.dateConfirmPayments.heading"))
      doc.select("p").text  must include(messages("monthlyreturns.dateConfirmPayments.paragraph"))

      doc.select(".govuk-warning-text__text").text must include(
        messages("monthlyreturns.dateConfirmPayments.warning")
      )
      val linkText = doc.getElementsByClass("govuk-link").eachText()
      linkText must contain(messages("monthlyreturns.dateConfirmPayments.p1.link"))

      val deleteReturnLink =
        doc.select(s"a:contains(${messages("monthlyreturns.dateConfirmPayments.p1.link")})")
      deleteReturnLink.attr("href") mustBe
        app.injector.instanceOf[FrontendAppConfig].incompleteReturnsUrl

      doc.getElementsByClass("govuk-button").text must include(messages("site.continue"))
    }

    "must show one summary link, to the first field with an error, matching the inline message" in new Setup {
      val boundForm = formProvider().bind(Map("value.month" -> "", "value.year" -> ""))
      val doc       =
        Jsoup.parse(view(boundForm, NormalMode, "monthlyreturns.dateConfirmPayments", MonthlyStandardReturn).toString)

      val monthMsg = messages("monthlyreturns.dateConfirmNilPayments.error.required.month")
      val yearMsg  = messages("monthlyreturns.dateConfirmNilPayments.error.required.year")

      val links = doc.select(".govuk-error-summary__list a")
      links.size mustBe 1
      links.text mustBe monthMsg
      links.attr("href") mustBe "#value.month"
      doc.select(".govuk-error-message").text must include(monthMsg)
      doc.select(".govuk-error-message").text must not include yearMsg
    }

    "must link the summary to the year field when only the year has an error" in new Setup {
      val boundForm = formProvider().bind(Map("value.month" -> "Nov", "value.year" -> ""))
      val doc       =
        Jsoup.parse(view(boundForm, NormalMode, "monthlyreturns.dateConfirmPayments", MonthlyStandardReturn).toString)

      val links = doc.select(".govuk-error-summary__list a")
      links.size mustBe 1
      links.attr("href") mustBe "#value.year"
      doc.select(".govuk-error-message").text mustBe s"Error: ${links.text}"
    }

    "must show the same message in the summary and inline when month is invalid and year is missing" in new Setup {
      val boundForm = formProvider().bind(Map("value.month" -> "jany", "value.year" -> ""))
      val doc       =
        Jsoup.parse(view(boundForm, NormalMode, "monthlyreturns.dateConfirmPayments", MonthlyStandardReturn).toString)

      val links = doc.select(".govuk-error-summary__list a")
      links.size mustBe 1
      links.text mustBe messages("monthlyreturns.dateConfirmNilPayments.error.invalid.month")
      links.attr("href") mustBe "#value.month"
      doc.select(".govuk-error-message").text mustBe s"Error: ${links.text}"
    }

    "must render the legend heading with a reduced bottom margin so the hint sits close to it" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      val legendHeading = doc.select("legend h2")
      legendHeading.text mustBe messages("monthlyreturns.dateConfirmPayments.legendText")
      legendHeading.hasClass("govuk-!-margin-bottom-1") mustBe true
    }

    "must set autocomplete=off on the month and year inputs, as they do not hold personal data" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[name$=month]").attr("autocomplete") mustBe "off"
      doc.select("input[name$=year]").attr("autocomplete") mustBe "off"
    }

    "must display error summary when form has errors" in new Setup {
      val formWithError = form.withError("value", "monthlyreturns.dateConfirmPayments.error.duplicate")
      val htmlWithError = view(
        formWithError,
        NormalMode,
        "monthlyreturns.dateConfirmPayments",
        MonthlyStandardReturn
      )
      val doc: Document = Jsoup.parse(htmlWithError.toString)

      doc.select(".govuk-error-summary").size mustBe 1
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[DateConfirmPaymentsView]
    val mockFrontendAppConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"
    val clock: Clock                              = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneId.of("Europe/London"))
    val formProvider                              = new DateConfirmPaymentsFormProvider(mockFrontendAppConfig, clock)
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view(
      form,
      NormalMode,
      "monthlyreturns.dateConfirmPayments",
      MonthlyStandardReturn
    )
  }
}
