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
import models.agent.AgentClientData
import models.submission.SubmissionDetails
import models.{ReturnType, UserAnswers}
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
import services.guard.SubmissionSuccessfulCheck.{GuardFailed, GuardPassed}
import services.guard.SubmissionSuccessfulServiceGuard
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Locale
import scala.concurrent.Future

class SubmittedNoReceiptControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val email: String              = "test@test.com"
  private val fallbackEmail: String      = "fallback@test.com"
  private val periodEnd: LocalDate       = LocalDate.of(2018, 3, 5)
  private val fixedInstant: Instant      = Instant.parse("2017-01-06T08:46:00Z")
  private val contractorName: String     = "PAL 355 Scheme"
  private val employerRef: String        = "taxOfficeNumber/taxOfficeReference"
  private val submissionType: ReturnType = ReturnType.MonthlyNilReturn

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

  private val mockGuard: SubmissionSuccessfulServiceGuard =
    mock(classOf[SubmissionSuccessfulServiceGuard])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMonthlyReturnService, mockGuard)
  }

  private val submittedNoReceiptDetails: SubmissionDetails =
    SubmissionDetails(id = "sub-123", status = "SUBMITTED_NO_RECEIPT", irMark = "", submittedAt = Instant.now())

  private def baseUa(
    withEmail: Boolean = true,
    withContractorName: Boolean = true,
    withPeriodEnd: Boolean = true,
    withReturnType: Boolean = true
  ): UserAnswers = {
    val start = userAnswersWithCisId
      .set(SubmissionDetailsPage, submittedNoReceiptDetails)
      .success
      .value

    val ua1 =
      if (withContractorName) start.set(ContractorNamePage, contractorName).success.value else start

    val ua2 =
      if (withEmail) ua1.set(EnterYourEmailAddressPage, email).success.value
      else ua1.remove(EnterYourEmailAddressPage).success.value

    val ua3 =
      if (withPeriodEnd) ua2.set(DateConfirmPaymentsPage, periodEnd).success.value else ua2

    if (withReturnType) ua3.set(ReturnTypePage, submissionType).success.value else ua3
  }

  private def buildApp(
    ua: UserAnswers,
    isAgent: Boolean = false,
    hasEmployeeRef: Boolean = true,
    hasAgentRef: Boolean = true
  ): Application =
    applicationBuilder(
      userAnswers = Some(ua),
      isAgent = isAgent,
      hasEmployeeRef = hasEmployeeRef,
      hasAgentRef = hasAgentRef,
      additionalBindings = Seq(
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
        bind[MonthlyReturnService].toInstance(mockMonthlyReturnService),
        bind[SubmissionSuccessfulServiceGuard].toInstance(mockGuard)
      )
    ).build()

  private lazy val request =
    FakeRequest(GET, routes.SubmittedNoReceiptController.onPageLoad.url)

  "SubmittedNoReceiptController" - {

    "contractor" - {

      "onPageLoad must return OK and render key fields" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val app = buildApp(baseUa())

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK

          val body = contentAsString(result)
          body must include(periodEnd.format(monthYearFmt))
          body must include(submittedTime)
          body must include(submittedDate)
          body must include(contractorName)
          body must include(employerRef)
          body must include(email)

          verifyNoInteractions(mockMonthlyReturnService)
        }
      }

      "onPageLoad must redirect to JourneyRecovery when guard fails" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardFailed))

        val app = buildApp(baseUa())

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Unauthorised Organisation Affinity if cisId is missing from UserAnswers" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val app = buildApp(emptyUserAnswers)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.UnauthorisedOrganisationAffinityController
            .onPageLoad()
            .url
        }
      }

      "must throw if contractorName missing" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val app = buildApp(baseUa(withContractorName = false))

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("contractorName missing for userId=")
        }
      }

      "must throw if taxPeriodEnd is missing" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val app = buildApp(baseUa(withPeriodEnd = false))

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers")
        }
      }

      "must throw if ReturnTypePage is missing" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val app = buildApp(baseUa(withReturnType = false))

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
        }
      }

      "must call monthlyReturnService and use returned email when EnterYourEmailAddressPage is missing" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        when(mockMonthlyReturnService.getSchemeEmail(eqTo("1"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(fallbackEmail)))

        val app = buildApp(baseUa(withEmail = false))

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK

          val body = contentAsString(result)
          body must include(fallbackEmail)

          verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
        }
      }

      "must throw if employerReference is missing" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val app = buildApp(baseUa(), hasEmployeeRef = false)

        running(app) {
          val ex = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          ex.getMessage must include("employerReference missing for userId=")
        }
      }
    }

    "agent" - {

      "onPageLoad must return OK and render key fields using AgentClientData" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val agentData =
          AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some(contractorName))

        val ua =
          baseUa()
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
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val agentDataMissing =
          AgentClientData("CLIENT-123", "", "taxOfficeReference", Some(contractorName))

        val ua =
          baseUa()
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

      "must call monthlyReturnService and use returned email when EnterYourEmailAddressPage is missing (agent)" in {
        when(mockGuard.check(any())).thenReturn(Future.successful(GuardPassed))

        val agentData =
          AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some(contractorName))

        val ua =
          baseUa(withEmail = false)
            .set(AgentClientDataPage, agentData)
            .success
            .value

        when(mockMonthlyReturnService.getSchemeEmail(eqTo("1"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(fallbackEmail)))

        val app = buildApp(ua, isAgent = true)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          contentAsString(result) must include(fallbackEmail)

          verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
        }
      }
    }
  }
}
