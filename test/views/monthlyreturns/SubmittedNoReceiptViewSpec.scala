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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.SubmittedNoReceiptView

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}

class SubmittedNoReceiptViewSpec extends SpecBase {

  "SubmittedNoReceiptView" - {
    "must render the correct content on the page" in new Setup {
      val html: HtmlFormat.Appendable = view(periodEnd, submittedTime, submittedDate, contractorName, empRef, email)
      val doc: Document = Jsoup.parse(html.body)

      doc.title             must include(messages("submittedNoReceipt.title"))
      doc.select("h1").text must include(messages("submittedNoReceipt.heading"))
      doc.select("p").text  must include(
        messages(
          s"Your CIS monthly return for period ended $periodEnd was successfully submitted to HMRC at $submittedTime on $submittedDate"
        )
      )
      doc.select("h2").text must include(messages("submittedNoReceipt.details.h2"))
      doc.select("dt").text must include(messages("submittedNoReceipt.details.contractorName"))
      doc.select("dd").text must include(messages(contractorName))
      doc.select("dt").text must include(messages("submittedNoReceipt.details.employersReference"))
      doc.select("dd").text must include(messages(empRef))
      doc.select("dt").text must include(messages("submittedNoReceipt.details.emailConfirmationSentTo"))
      doc.select("dd").text must include(messages(email))

      doc.select("h2").text                     must include(messages("submittedNoReceipt.h2"))
      doc.select("p").text                      must include(messages("submittedNoReceipt.p2"))
      doc.select("p").text                      must include(
        messages(
          s"We were unable to issue a submission receipt at this time. HMRC will contact you shortly. " +
            s"You will receive confirmation of your submission at the following email address: $email"
        )
      )
      doc.select("p").text                      must include(messages("submittedNoReceipt.p4"))
      doc.getElementsByClass("govuk-link").text must include(messages("submittedNoReceipt.p4.link"))
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: SubmittedNoReceiptView              = app.injector.instanceOf[SubmittedNoReceiptView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    private val london                = ZoneId.of("Europe/London")
    private val fixedInstant: Instant = Instant.parse("2017-01-06T08:46:00Z")
    private val ukNow: ZonedDateTime  = ZonedDateTime.ofInstant(fixedInstant, london)
    private val dmyFmt                = DateTimeFormatter.ofPattern("d MMM uuuu")
    private val timeFmt               = DateTimeFormatter.ofPattern("HH:mm z")
    private val year                  = 2018
    private val month                 = 3
    private val dayOfMonth            = 5

    val periodEnd: String     = LocalDate.of(year, month, dayOfMonth).format(dmyFmt)
    val submittedTime: String = ukNow.format(timeFmt)
    val submittedDate: String = ukNow.format(dmyFmt)
    val contractorName        = "PAL 355 Scheme"
    val empRef                = "123/AB456"
    val email                 = "test@test.com"
  }
}
