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
import models.ReturnType.MonthlyNilReturn
import models.UserAnswers
import models.agent.AgentClientData
import models.submission.SubmissionDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{ContractorNamePage, DateConfirmPaymentsPage, EnterYourEmailAddressPage, ReturnTypePage}
import pages.submission.SubmissionDetailsPage
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import utils.IrMarkReferenceGenerator

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Locale
import scala.concurrent.Future

class SubmissionSuccessControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val email: String         = "test@test.com"
  private val fallbackEmail: String = "fallback@test.com"
  private val periodEnd: LocalDate  = LocalDate.of(2018, 3, 5)

  private val fixedInstant: Instant = Instant.parse("2017-01-06T08:46:00Z")

  private val irMarkBase64: String = "Pyy1LRJh053AE+nuyp0GJR7oESw="
  private val reference: String    = IrMarkReferenceGenerator.fromBase64(irMarkBase64)

  private val contractorName: String = "PAL 355 Scheme"
  private val employerRef: String    = "taxOfficeNumber/taxOfficeReference"

  private val london: ZoneId = ZoneId.of("Europe/London")

  private val monthYearFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK)

  private val dayMonthYearFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK)

  private val submittedTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mma", Locale.UK)

  private val ukNow: ZonedDateTime =
    ZonedDateTime.ofInstant(fixedInstant, london)

  private val submittedTime: String =
    ukNow.format(submittedTimeFmt).toLowerCase(Locale.UK)

  private val submittedDate: String =
    ukNow.format(dayMonthYearFmt)

  private val mockMonthlyReturnService: MonthlyReturnService =
    mock(classOf[MonthlyReturnService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMonthlyReturnService)
  }

  private def submissionDetails(
    status: String = "ACCEPTED",
    irMark: String = irMarkBase64
  ): SubmissionDetails =
    SubmissionDetails(id = "123", status = status, irMark = irMark, submittedAt = Instant.now)

  private def baseUa(
    withReturnType: Boolean = true,
    withEmail: Option[String] = Some(email),
    withContractorName: Boolean = true,
    withPeriodEnd: Boolean = true,
    withSubmissionDetails: Boolean = true,
    submissionStatus: String = "ACCEPTED",
    irMark: String = irMarkBase64
  ): UserAnswers = {
    val start = userAnswersWithCisId

    val ua1 = if (withContractorName) start.set(ContractorNamePage, contractorName).success.value else start

    val ua2 = withEmail match {
      case Some(e) => ua1.set(EnterYourEmailAddressPage, e).success.value
      case None    => ua1.remove(EnterYourEmailAddressPage).success.value
    }

    val ua3 = if (withReturnType) ua2.set(ReturnTypePage, MonthlyNilReturn).success.value else ua2
    val ua4 = if (withPeriodEnd) ua3.set(DateConfirmPaymentsPage, periodEnd).success.value else ua3

    if (withSubmissionDetails)
      ua4.set(SubmissionDetailsPage, submissionDetails(submissionStatus, irMark)).success.value
    else
      ua4.remove(SubmissionDetailsPage).success.value
  }

  private def buildApp(
    userAnswers: UserAnswers,
    isAgent: Boolean = false,
    hasEmployeeRef: Boolean = true,
    hasAgentRef: Boolean = true
  ): Application =
    applicationBuilder(
      userAnswers = Some(userAnswers),
      isAgent = isAgent,
      hasEmployeeRef = hasEmployeeRef,
      hasAgentRef = hasAgentRef,
      additionalBindings = Seq(
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
        bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
      )
    ).build()

  private lazy val request = FakeRequest(GET, routes.SubmissionSuccessController.onPageLoad.url)

  "SubmissionSuccessController" - {

    "contractor" - {

      "must return OK and render key fields" in {
        val app = buildApp(baseUa())

        def referenceWithBreaks(ref: String): String =
          ref.grouped(22).mkString("<br>")

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK

          val body = contentAsString(result)
          body must include(referenceWithBreaks(reference))
          body must include(periodEnd.format(monthYearFmt))
          body must include(submittedDate)
          body must include(submittedTime)
          body must include(contractorName)
          body must include(employerRef)
          body must include(email)
        }
      }

      "must not call getSchemeEmail when email is present in user answers" in {
        val app = buildApp(baseUa(withEmail = Some(email)))

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          verifyNoInteractions(mockMonthlyReturnService)
        }
      }

      "must call getSchemeEmail and use returned email when EnterYourEmailAddressPage is missing" in {
        when(mockMonthlyReturnService.getSchemeEmail(eqTo("1"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(fallbackEmail)))

        val app = buildApp(baseUa(withEmail = None))

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK

          val body = contentAsString(result)
          body must include(fallbackEmail)

          verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
        }
      }

      "must default to empty email if getSchemeEmail fails" in {
        when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val app = buildApp(baseUa(withEmail = None))

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK

          verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
        }
      }

      "must redirect to Unauthorised Organisation Affinity if cisId is missing" in {
        val app = buildApp(emptyUserAnswers)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.UnauthorisedOrganisationAffinityController
            .onPageLoad()
            .url
        }
      }

      "must throw if ReturnTypePage is missing" in {
        val app = buildApp(baseUa(withReturnType = false))

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("[SubmissionSuccess] ReturnTypePage missing from userAnswers")
        }
      }

      "must throw if contractorName is missing" in {
        val app = buildApp(baseUa(withContractorName = false))

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("contractorName missing for userId=")
        }
      }

      "must throw if employerReference is missing" in {
        val app = buildApp(baseUa(), hasEmployeeRef = false)

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("employerReference missing for userId=")
        }
      }

      "must throw if taxPeriodEnd is missing" in {
        val app = buildApp(baseUa(withPeriodEnd = false))

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("[SubmissionSuccess] taxPeriodEnd missing from userAnswers")
        }
      }

      "must redirect to JourneyRecovery when guard fails (missing submission details)" in {
        val app = buildApp(baseUa(withSubmissionDetails = false))

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecovery when guard fails (non-success status + empty irMark)" in {
        val app = buildApp(baseUa(submissionStatus = "PENDING", irMark = ""))

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "agent" - {

      "must return OK and render key fields using AgentClientData" in {
        val agentData = AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some(contractorName))

        val ua = baseUa()
          .set(AgentClientDataPage, agentData)
          .success
          .value

        val app = buildApp(ua, isAgent = true)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK

          val body = contentAsString(result)
          body must include(contractorName)
          body must include(employerRef)
        }
      }

      "must throw if agent employerReference is missing" in {
        val agentDataMissing = AgentClientData("CLIENT-123", "", "taxOfficeReference", Some(contractorName))

        val ua = baseUa()
          .set(AgentClientDataPage, agentDataMissing)
          .success
          .value

        val app = buildApp(ua, isAgent = true, hasAgentRef = false)

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("employerReference missing for userId=")
        }
      }
    }
  }
}
