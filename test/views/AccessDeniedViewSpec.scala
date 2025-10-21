package views

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.AccessDeniedView

class AccessDeniedViewSpec extends SpecBase {

  "AccessDeniedView" - {

    "must render the page with the correct title, heading and link" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title                                 must include(messages("accessDenied.title"))
      doc.select("h1").text                     must include(messages("accessDenied.heading"))
      doc.getElementsByClass("govuk-link").text must include(messages("accessDenied.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[AccessDeniedView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
