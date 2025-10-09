package views.monthlyreturns

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.UnauthorisedAgentView

class UnauthorisedAgentViewSpec extends SpecBase with Matchers {

  "UnauthorisedAgentViewSpec" - {

    "must render the page with the correct heading and paragraph" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title             must include(messages("monthlyreturns.unauthorised.agent.title"))
      doc.select("h1").text must include(messages("monthlyreturns.unauthorised.agent.heading"))
      doc.select("p").text  must include(messages("monthlyreturns.unauthorised.agent.p1"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[UnauthorisedAgentView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
