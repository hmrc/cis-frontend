package views.monthlyreturns

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.AlreadySubmittedView

class AlreadySubmittedViewSpec extends SpecBase with Matchers {

  "AlreadySubmittedView" - {

    "must render the page with the correct heading and paragraph with link" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title                                 must include(messages("monthlyreturns.alreadySubmitted.title"))
      doc.select("h1").text                     must include(messages("monthlyreturns.alreadySubmitted.heading"))
      doc.select("p").text                      must include(messages("monthlyreturns.alreadySubmitted.paragraph"))
      doc.getElementsByClass("govuk-link").text must include(messages("monthlyreturns.alreadySubmitted.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[AlreadySubmittedView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
