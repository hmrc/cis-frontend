package views.monthlyreturns

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.UnauthorisedOrgView

class UnauthorisedOrgViewSpec extends SpecBase with Matchers {

  "UnauthorisedOrgViewSpec" - {

    "must render the page with correct heading, paragraphs, and link" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title                                 must include(messages("monthlyreturns.unauthorised.org.title"))
      doc.select("h1").text                     must include(messages("monthlyreturns.unauthorised.org.heading"))
      doc.select("p").text                      must include(messages("monthlyreturns.unauthorised.org.guidance.p1"))
      doc.select("p").text                      must include(messages("monthlyreturns.unauthorised.org.guidance.p2"))
      doc.getElementsByClass("govuk-link").text must include(messages("monthlyreturns.unauthorised.org.guidance.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[UnauthorisedOrgView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
