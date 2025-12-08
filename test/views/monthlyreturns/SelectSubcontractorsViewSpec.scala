package views.monthlyreturns

import base.SpecBase
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.monthlyreturns.SelectSubcontractorsFormData
import org.jsoup.Jsoup
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.FakeRequest
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

class SelectSubcontractorsViewSpec extends SpecBase {

  "SelectSubcontractorsView" - {
    "must render the correct page title and heading" in new Setup {
      val html = view(form, subcontractors)
      val doc = Jsoup.parse(html.body)

      doc.title must include(messages("monthlyreturns.selectSubcontractors.title"))
      doc.select("h1").text mustBe messages("monthlyreturns.selectSubcontractors.heading")
    }

    "must render the introductory paragraphs and the commit-selection checkbox label" in new Setup {
      val html = view(form, subcontractors)
      val doc = Jsoup.parse(html.body)

      doc.select("p").text must include(messages("monthlyreturns.selectSubcontractors.p1"))
      doc.select("p").text must include(messages("monthlyreturns.selectSubcontractors.p2"))

      val checkboxLabel = doc.select("div.govuk-checkboxes label").text
      checkboxLabel must include(messages("monthlyreturns.selectSubcontractors.checkbox.label"))
    }

    "must render the correct links for add subcontractor, select all and deselect all" in new Setup {
      val html = view(form, subcontractors)
      val doc = Jsoup.parse(html.body)
      val linkText = doc.getElementsByClass("govuk-link").eachText()

      linkText must contain(messages("monthlyreturns.selectSubcontractors.p2.link"))
      linkText must contain(messages("monthlyreturns.selectSubcontractors.selectAll.link"))
      linkText must contain(messages("monthlyreturns.selectSubcontractors.deselectAll.link"))
    }

    "must render the table headers in the correct order" in new Setup {
      val html = view(form, subcontractors)
      val doc = Jsoup.parse(html.body)
      val headers = doc.select("table thead th").eachText()

      headers must contain theSameElementsInOrderAs Seq(
        messages("monthlyreturns.selectSubcontractors.table.th.name"),
        messages("monthlyreturns.selectSubcontractors.table.th.verificationRequired"),
        messages("monthlyreturns.selectSubcontractors.table.th.verificationNumber"),
        messages("monthlyreturns.selectSubcontractors.table.th.taxTreatment"),
        messages("monthlyreturns.selectSubcontractors.table.th.includeThisMonth")
      )
    }

    "must render the correct number of subcontractor rows in the table" in new Setup {
      val html = view(form, subcontractors)
      val doc = Jsoup.parse(html.body)

      val rows = doc.select("table tbody tr")
      rows.size mustBe subcontractors.size
    }
  }

  trait Setup {
    val app: Application                               = applicationBuilder().build()
    val view: SelectSubcontractorsView                 = app.injector.instanceOf[SelectSubcontractorsView]
    val formProvider: SelectSubcontractorsFormProvider = app.injector.instanceOf[SelectSubcontractorsFormProvider]
    val form: Form[SelectSubcontractorsFormData]       = formProvider()
    implicit val request: play.api.mvc.Request[_]      = FakeRequest()
    implicit val messages: Messages                    = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val subcontractors = Seq(
      SelectSubcontractorsViewModel("Alice, A", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Bob, B", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Charles, C", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Dave, D", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Elise, E", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Frank, F", "Yes", "Unknown", "Unknown", false)
    )
  }
}
