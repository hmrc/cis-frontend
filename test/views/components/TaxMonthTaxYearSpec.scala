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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Messages, MessagesApi}
import play.twirl.api.HtmlFormat
import views.html.components.TaxMonthTaxYear

class TaxMonthTaxYearSpec extends PlaySpec with GuiceOneAppPerSuite {

  "TaxMonthTaxYear component" should {

    "render correctly with no errors" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "3",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementById("taxMonth").attr("value") must include("3")
      html.getElementById("taxYear").attr("value")  must include("2024")
      html.getElementsByTag("input").attr("class") mustNot include("govuk-input--error")
    }

    "render correctly with month error" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementById("taxMonth").attr("value") mustNot include("3")
      html.getElementById("taxYear").attr("value")        must include("2024")
      html.getElementsByTag("input").get(0).attr("class") must include("govuk-input--error")
    }

    "render correctly with year error" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "3",
          "taxYear"  -> ""
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementById("taxMonth").attr("value")       must include("3")
      html.getElementById("taxYear").attr("value") mustNot include("2024")
      html.getElementsByTag("input").get(1).attr("class") must include("govuk-input--error")
    }

    "render correctly with both month and year errors" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "",
          "taxYear"  -> ""
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementById("taxMonth").attr("value") mustNot include("3")
      html.getElementById("taxYear").attr("value") mustNot include("2024")
      html.getElementsByTag("input").attr("class") must include("govuk-input--error")
    }

    "render correct labels for month and year" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "3",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.text() must include(monthLabel)
      html.text() must include(yearLabel)
    }

    "render with correct CSS classes for inputs" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "3",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementById("taxMonth").attr("class") must include("govuk-input--width-10")
      html.getElementById("taxYear").attr("class")  must include("govuk-input--width-4")
    }

    "render hint text when provided" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "3",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel,
        hintText = Some("For example, 3 2024")
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementsByClass("govuk-hint").size() mustBe 1
    }

    "render error message when formError is provided" in new Setup {
      val formWithValues: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "3",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val formError = play.api.data.FormError("taxMonthAndYear", "error.duplicate")

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithValues("taxMonth"),
        yearField = formWithValues("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel,
        formError = Some(formError)
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      html.getElementsByClass("govuk-error-message").size() mustBe 1
    }

    "prioritise field errors over formError" in new Setup {
      val formWithErrors: Form[TestModel] = form.bind(
        Map(
          "taxMonth" -> "",
          "taxYear"  -> "2024"
        )
      )

      val taxMonthTaxYearView = app.injector.instanceOf[TaxMonthTaxYear]

      val formError = play.api.data.FormError("taxMonthAndYear", "error.duplicate")

      val output: HtmlFormat.Appendable = taxMonthTaxYearView(
        monthField = formWithErrors("taxMonth"),
        yearField = formWithErrors("taxYear"),
        monthLabel = monthLabel,
        yearLabel = yearLabel,
        formError = Some(formError)
      )(messages)

      val html: Document = Jsoup.parse(output.toString)

      // Should show field error, not formError
      html.getElementsByClass("govuk-error-message").size() mustBe 1
      html.getElementById("taxMonth").attr("class") must include("govuk-input--error")
    }

    trait Setup {
      val messagesApi: MessagesApi    = app.injector.instanceOf[MessagesApi]
      implicit val messages: Messages = messagesApi.preferred(Seq.empty)

      case class TestModel(taxMonth: Int, taxYear: Int)

      val form: Form[TestModel] = Form(
        mapping(
          "taxMonth" -> number,
          "taxYear"  -> number
        )((month, year) => TestModel(month, year))(model => Some((model.taxMonth, model.taxYear)))
      )

      val monthLabel: String = "Month"
      val yearLabel: String  = "Year"
    }
  }
}
