package views

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.SystemErrorView

class SystemErrorViewSpec extends SpecBase {

  "SystemErrorView" - {

    "must render the page with the correct paragraphs, link and incident reference number" in new Setup {
      val incidentReferenceNumber = "P3JXIAHME5VASC10"
      val html                    = view(incidentReferenceNumber)
      val doc                     = Jsoup.parse(html.body)

      doc.title                                 must include(messages("systemError.title"))
      doc.select("h1").text                     must include(messages("systemError.heading"))
      doc.select("p").text                      must include(messages("systemError.p1"))
      doc.select("p").text                      must include(messages("systemError.p2"))
      doc.select("p").text                      must include(messages("systemError.p3"))
      doc.getElementsByClass("govuk-link").text must include(messages("systemError.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[SystemErrorView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
