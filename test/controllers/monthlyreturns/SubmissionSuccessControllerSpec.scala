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

package controllers.monthlyreturns

import base.SpecBase
import models.UserAnswers
import pages.monthlyreturns.{ConfirmEmailAddressPage, DateConfirmNilPaymentsPage}
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import views.html.monthlyreturns.SubmissionSuccessView

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId, ZoneOffset, ZonedDateTime}

class SubmissionSuccessControllerSpec extends SpecBase {

  "SubmissionSuccessController.onPageLoad" - {

    "must return OK and render the expected view" in new Setup {
      running(app) {
        val result = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe expectedHtml
      }
    }
  }

  trait Setup {
    val email: String          = "test@test.com"
    val periodEnd: LocalDate   = LocalDate.of(2018, 3, 5)
    val fixedInstant: Instant  = Instant.parse("2017-01-06T08:46:00Z")
    val reference: String      = "KCNJQEFYOYVU6C2BTZCDQSWUSGG5ODG"
    val contractorName: String = "PAL 355 Scheme"
    val employerRef: String    = "taxOfficeNumber/taxOfficeReference"

    private val dmyFmt  = DateTimeFormatter.ofPattern("d MMM uuuu")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm z")
    private val london  = ZoneId.of("Europe/London")

    protected lazy val ukNow: ZonedDateTime =
      ZonedDateTime.ofInstant(fixedInstant, london)

    protected lazy val submittedTime: String =
      ukNow.format(timeFmt)

    protected lazy val submittedDate: String =
      ukNow.format(dmyFmt)

    lazy val ua: UserAnswers =
      emptyUserAnswers
        .set(ConfirmEmailAddressPage, email)
        .success
        .value
        .set(DateConfirmNilPaymentsPage, periodEnd)
        .success
        .value

    lazy val app: Application =
      applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)))
        .build()

    lazy val request: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(GET, routes.SubmissionSuccessController.onPageLoad.url)
    lazy val view: SubmissionSuccessView                  = app.injector.instanceOf[SubmissionSuccessView]

    lazy val expectedHtml: String =
      view(
        reference = reference,
        periodEnd = periodEnd.format(dmyFmt),
        submittedTime = submittedTime,
        submittedDate = submittedDate,
        contractorName = contractorName,
        empRef = employerRef,
        email = email
      )(request, messages(app)).toString
  }
}
