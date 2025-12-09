package views.monthlyreturns

import base.SpecBase
import org.apache.pekko.actor.setup.Setup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.FileYourMonthlyCisReturnView

class FileYourMonthlyCisReturnViewSpec extends SpecBase {

  "FileYourMonthlyCisReturnView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[FileYourMonthlyCisReturnView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view()
  }
}