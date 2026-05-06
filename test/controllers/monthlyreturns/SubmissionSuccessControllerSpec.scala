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
import models.{ReturnType, UserAnswers}
import models.agent.AgentClientData
import models.submission.SubmissionDetails
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatest.BeforeAndAfterEach
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{ContractorNamePage, DateConfirmPaymentsPage, EnterYourEmailAddressPage, ReturnTypePage}
import pages.submission.SubmissionDetailsPage
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import services.MonthlyReturnService
import services.guard.SubmissionSuccessfulServiceGuard
import uk.gov.hmrc.http.HeaderCarrier
import utils.IrMarkReferenceGenerator

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Locale
import scala.concurrent.Future

class SubmissionSuccessControllerSpec extends SpecBase with BeforeAndAfterEach {

  val email: String          = "test@test.com"
  val periodEnd: LocalDate   = LocalDate.of(2018, 3, 5)
  val fixedInstant: Instant  = Instant.parse("2017-01-06T08:46:00Z")
  val irMarkBase64: String   = "Pyy1LRJh053AE+nuyp0GJR7oESw="
  val reference: String      = IrMarkReferenceGenerator.fromBase64(irMarkBase64)
  val contractorName: String = "PAL 355 Scheme"
  val employerRef: String    = "taxOfficeNumber/taxOfficeReference"

  private val monthYearFmt             = DateTimeFormatter.ofPattern("MMMM uuuu").withLocale(Locale.UK)
  private val fullDateFmt              = DateTimeFormatter.ofPattern("d MMMM uuuu").withLocale(Locale.UK)
  private val timeFmt                  = DateTimeFormatter.ofPattern("h:mma").withLocale(Locale.UK)
  private val london                   = ZoneId.of("Europe/London")
  private val mockMonthlyReturnService = mock(classOf[MonthlyReturnService])
  private val mockGuard                = mock(classOf[SubmissionSuccessfulServiceGuard])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMonthlyReturnService, mockGuard)
  }

  protected lazy val ukNow: ZonedDateTime =
    ZonedDateTime.ofInstant(fixedInstant, london)

  protected lazy val submittedTime: String =
    ukNow.format(timeFmt)

  protected lazy val submittedDate: String =
    ukNow.format(fullDateFmt)

  val ua: UserAnswers =
    userAnswersWithCisId
      .set(ContractorNamePage, contractorName)
      .success
      .value
      .set(EnterYourEmailAddressPage, email)
      .success
      .value
      .set(DateConfirmPaymentsPage, periodEnd)
      .success
      .value
      .set(
        SubmissionDetailsPage,
        SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
      )
      .success
      .value

  lazy val agentDate: AgentClientData =
    AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some("PAL 355 Scheme"))

  lazy val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routes.SubmissionSuccessController.onPageLoad.url)

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
      hasAgentRef = hasAgentRef
    )
      .overrides(
        bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
        bind[MonthlyReturnService].toInstance(mockMonthlyReturnService),
        bind[SubmissionSuccessfulServiceGuard].toInstance(mockGuard)
      )
      .build()

  "SubmissionSuccessController" - {

    "contractor" - {

      val userAnswersWithReturnType = ua
        .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
        .success
        .value

      "must return OK and render key fields" in {
        when(mockGuard.check(any())).thenReturn(true)
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val app = buildApp(userAnswersWithReturnType)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          val body   = contentAsString(result)
          body must include(periodEnd.format(monthYearFmt))
          body must include(submittedDate)
          body must include(contractorName)
          body must include(employerRef)
          body must include(email)
        }
      }

      "must not call getSchemeEmail when email is present in user answers" in {
        when(mockGuard.check(any())).thenReturn(true)
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val app = buildApp(userAnswersWithReturnType)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          verify(mockMonthlyReturnService, never()).getSchemeEmail(any())(any())
        }
      }

      "must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(app) {
          val result = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.UnauthorisedOrganisationAffinityController
            .onPageLoad()
            .url
        }
      }

      "must call getSchemeEmail and use returned email when EnterYourEmailAddressPage is missing" in {
        val fallbackEmail  = "fallback@test.com"
        val uaWithoutEmail = userAnswersWithCisId
          .set(ContractorNamePage, contractorName)
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage, periodEnd)
          .success
          .value
          .set(
            SubmissionDetailsPage,
            SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
          )
          .success
          .value

        when(mockGuard.check(any())).thenReturn(true)
        when(mockMonthlyReturnService.getSchemeEmail(eqTo("1"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(fallbackEmail)))
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val app = buildApp(uaWithoutEmail)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          contentAsString(result) must include(fallbackEmail)
          verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
        }
      }

      "must default to empty email if getSchemeEmail fails" in {
        val uaWithoutEmail = userAnswersWithCisId
          .set(ContractorNamePage, contractorName)
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(DateConfirmPaymentsPage, periodEnd)
          .success
          .value
          .set(
            SubmissionDetailsPage,
            SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
          )
          .success
          .value

        when(mockGuard.check(any())).thenReturn(true)
        when(mockMonthlyReturnService.getSchemeEmail(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val app = buildApp(uaWithoutEmail)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
        }
      }

      "must throw if ReturnTypePage is missing" in {
        when(mockGuard.check(any())).thenReturn(true)
        val app = buildApp(ua)

        running(app) {
          val thrown = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          thrown.getMessage must include("[SubmissionSuccess] ReturnTypePage missing from userAnswers")
        }
      }

      "must throw if contractorName is missing" in {
        when(mockGuard.check(any())).thenReturn(true)
        val incompleteUa = userAnswersWithCisId
          .set(ReturnTypePage, MonthlyNilReturn)
          .success
          .value
          .set(EnterYourEmailAddressPage, email)
          .success
          .value
          .set(DateConfirmPaymentsPage, periodEnd)
          .success
          .value
          .set(
            SubmissionDetailsPage,
            SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
          )
          .success
          .value

        val app = buildApp(incompleteUa)

        running(app) {
          val thrown = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          thrown.getMessage must include("contractorName missing for userId=")
        }
      }

      "must throw if employerReference is missing" in {
        when(mockGuard.check(any())).thenReturn(true)
        val app = buildApp(userAnswersWithReturnType, hasEmployeeRef = false)

        running(app) {
          val thrown = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          thrown.getMessage must include("employerReference missing for userId=")
        }
      }

      "must throw if taxPeriodEnd is missing" in {
        when(mockGuard.check(any())).thenReturn(true)
        val incompleteUa = userAnswersWithCisId
          .set(ReturnTypePage, MonthlyNilReturn)
          .success
          .value
          .set(ContractorNamePage, contractorName)
          .success
          .value
          .set(EnterYourEmailAddressPage, "test@test.com")
          .success
          .value
          .set(
            SubmissionDetailsPage,
            SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
          )
          .success
          .value

        val app = buildApp(incompleteUa)

        running(app) {
          val thrown = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          thrown.getMessage must include("[SubmissionSuccess] taxPeriodEnd missing from userAnswers")
        }
      }

      "must redirect to JourneyRecovery when guard fails" in {
        when(mockGuard.check(any())).thenReturn(false)
        val app = buildApp(userAnswersWithReturnType)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecovery when submission details are missing (guard fails)" in {
        when(mockGuard.check(any())).thenReturn(false)
        val uaNoDetails = userAnswersWithCisId
          .set(ReturnTypePage, MonthlyNilReturn)
          .success
          .value

        val app = buildApp(uaNoDetails)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "agent" - {

      val userAnswersWithAgentClientData = ua
        .set(AgentClientDataPage, agentDate)
        .success
        .value

      val userAnswersWithReturnType = userAnswersWithAgentClientData
        .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
        .success
        .value

      "must return OK and render key fields using AgentClientData" in {
        when(mockGuard.check(any())).thenReturn(true)
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val app = buildApp(userAnswersWithReturnType, isAgent = true)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          val body   = contentAsString(result)
          body must include(contractorName)
          body must include(employerRef)
        }
      }

      "must redirect to Unauthorised Agent Affinity if cisId is not found in UserAnswer" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true).build()

        running(app) {
          val result = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.UnauthorisedAgentAffinityController
            .onPageLoad()
            .url
        }
      }

      "must throw if contractorName is missing" in {
        when(mockGuard.check(any())).thenReturn(true)
        val incompleteUa = userAnswersWithCisId
          .set(ReturnTypePage, MonthlyNilReturn)
          .success
          .value
          .set(EnterYourEmailAddressPage, email)
          .success
          .value
          .set(DateConfirmPaymentsPage, periodEnd)
          .success
          .value
          .set(
            SubmissionDetailsPage,
            SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
          )
          .success
          .value

        val app = buildApp(incompleteUa, isAgent = true)

        running(app) {
          val thrown = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          thrown.getMessage must include("contractorName missing for userId=")
        }
      }

      "must throw if agent employerReference is missing" in {
        when(mockGuard.check(any())).thenReturn(true)
        lazy val agentDateWithoutTaxRefTaxNumber: AgentClientData =
          AgentClientData("CLIENT-123", "", "taxOfficeReference", Some("PAL 355 Scheme"))

        val incompleteUa = userAnswersWithCisId
          .set(ReturnTypePage, MonthlyNilReturn)
          .success
          .value
          .set(EnterYourEmailAddressPage, email)
          .success
          .value
          .set(AgentClientDataPage, agentDateWithoutTaxRefTaxNumber)
          .success
          .value
          .set(DateConfirmPaymentsPage, periodEnd)
          .success
          .value
          .set(
            SubmissionDetailsPage,
            SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = LocalDateTime.now)
          )
          .success
          .value

        val app = buildApp(incompleteUa, isAgent = true, hasAgentRef = false)

        running(app) {
          val thrown = intercept[IllegalStateException] {
            await(route(app, request).get)
          }
          thrown.getMessage must include("employerReference missing for userId=")
        }
      }

      "must call getSchemeEmail when email is missing for agent" in {
        val fallbackEmail = "fallback@test.com"
        when(mockGuard.check(any())).thenReturn(true)
        val agentData     = AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some(contractorName))
        val agentUa       = ua
          .remove(EnterYourEmailAddressPage)
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value
          .set(AgentClientDataPage, agentData)
          .success
          .value

        when(mockMonthlyReturnService.getSchemeEmail(eqTo("1"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(fallbackEmail)))
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val app = buildApp(agentUa, isAgent = true)

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
