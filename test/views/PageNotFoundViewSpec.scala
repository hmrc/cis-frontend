package views

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.PageNotFoundView

class PageNotFoundViewSpec extends SpecBase with Matchers {

  "PageNotFoundView" - {

    "must render the page with the correct title, heading, paragraphs and link" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title                                 must include(messages("pageNotFound.title"))
      doc.select("h1").text                     must include(messages("pageNotFound.heading"))
      doc.select("p").text                      must include(messages("pageNotFound.p1"))
      doc.select("p").text                      must include(messages("pageNotFound.p2"))
      doc.select("p").text                      must include(messages("pageNotFound.p3"))
      doc.select("p").text                      must include(messages("pageNotFound.p4"))
      doc.getElementsByClass("govuk-link").text must include(messages("pageNotFound.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[PageNotFoundView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
