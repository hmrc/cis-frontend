package controllers.monthlyreturns

import base.SpecBase
import controllers.monthlyreturns
import models.UserAnswers
import pages.monthlyreturns.{ConfirmEmailAddressPage, ContractorNamePage, DateConfirmNilPaymentsPage}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.SubmittedNoReceiptView

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId, ZoneOffset, ZonedDateTime}

class SubmittedNoReceiptControllerSpec extends SpecBase {

  "SubmittedNoReceipt Controller" - {

    "must return OK and render the expected view" in new Setup {
      running(app) {
        val result = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe expectedHtml
      }
    }

    "must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in new Setup {

      private val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
      }
    }
  }

  trait Setup {
    val email: String          = "test@test.com"
    val periodEnd: LocalDate   = LocalDate.of(2018, 3, 5)
    val fixedInstant: Instant  = Instant.parse("2017-01-06T08:46:00Z")
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
      userAnswersWithCisId
        .set(ContractorNamePage, contractorName)
        .success
        .value
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
      FakeRequest(GET, routes.SubmittedNoReceiptController.onPageLoad().url)
    lazy val view: SubmittedNoReceiptView                 = app.injector.instanceOf[SubmittedNoReceiptView]

    lazy val expectedHtml: String =
      view(
        periodEnd = periodEnd.format(dmyFmt),
        submittedTime = submittedTime,
        submittedDate = submittedDate,
        contractorName = contractorName,
        empRef = employerRef,
        email = email
      )(request, messages(app)).toString
  }
}
