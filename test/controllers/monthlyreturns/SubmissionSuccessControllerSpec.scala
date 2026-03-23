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
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{ContractorNamePage, DateConfirmPaymentsPage, EnterYourEmailAddressPage, ReturnTypePage}
import pages.submission.SubmissionDetailsPage
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.monthlyreturns.SubmissionSuccessView
import utils.IrMarkReferenceGenerator

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Locale
import scala.concurrent.Future

class SubmissionSuccessControllerSpec extends SpecBase {

  val email: String              = "test@test.com"
  val periodEnd: LocalDate       = LocalDate.of(2018, 3, 5)
  val fixedInstant: Instant      = Instant.parse("2017-01-06T08:46:00Z")
  val irMarkBase64: String       = "Pyy1LRJh053AE+nuyp0GJR7oESw="
  val reference: String          = IrMarkReferenceGenerator.fromBase64(irMarkBase64)
  val contractorName: String     = "PAL 355 Scheme"
  val employerRef: String        = "taxOfficeNumber/taxOfficeReference"
  val submissionType: ReturnType = ReturnType.MonthlyNilReturn

  private val dmyFmt                   = DateTimeFormatter.ofPattern("MMMM uuuu")
  private val monthYearFmt             = DateTimeFormatter.ofPattern("MMMM uuuu").withLocale(Locale.UK)
  private val fullDateFmt              = DateTimeFormatter.ofPattern("d MMMM uuuu").withLocale(Locale.UK)
  private val timeFmt                  = DateTimeFormatter.ofPattern("h:mma").withLocale(Locale.UK)
  private val london                   = ZoneId.of("Europe/London")
  private val mockMonthlyReturnService = mock(classOf[MonthlyReturnService])

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
        SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
      )
      .success
      .value

  lazy val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routes.SubmissionSuccessController.onPageLoad.url)
  lazy val view: SubmissionSuccessView                  = app.injector.instanceOf[SubmissionSuccessView]

  lazy val expectedHtml: String =
    view(
      reference = reference,
      periodEnd = periodEnd.format(monthYearFmt),
      submittedTime = submittedTime,
      submittedDate = submittedDate,
      contractorName = contractorName,
      empRef = employerRef,
      email = email,
      submissionType = submissionType
    )(request, messages(app)).toString

  lazy val agentDate: AgentClientData =
    AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some("PAL 355 Scheme"))

  "SubmissionSuccessController" - {

    "contractor" - {

      "onPageLoad" - {

        val userAnswersWithReturnType = ua
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value

        lazy val app: Application =
          applicationBuilder(userAnswers = Some(userAnswersWithReturnType))
            .overrides(bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)))
            .build()

        "must return OK and render the expected view" in {
          running(app) {
            val result = route(app, request).value

            status(result) mustBe OK
            contentAsString(result) mustBe expectedHtml
          }
        }

        "must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in {

          val app = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(app) {

            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(
              result
            ).value mustEqual controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
          }
        }

        "must throw if ReturnTypePage is missing" in {
          val incompleteUa = ua // note: ua does not set ReturnTypePage

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()

          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("[SubmissionSuccess] ReturnTypePage missing from userAnswers")
          }
        }

        "must use scheme email when email is missing from user answers" in {
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
              SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
            )
            .success
            .value

          when(mockMonthlyReturnService.getSchemeEmail(eqTo("1"))(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(email)))

          val app =
            applicationBuilder(userAnswers = Some(uaWithoutEmail))
              .overrides(
                bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
                bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
              )
              .build()

          running(app) {
            val result = route(app, request).value

            status(result) mustBe OK
            contentAsString(result) must include(email)

            verify(mockMonthlyReturnService).getSchemeEmail(eqTo("1"))(any[HeaderCarrier])
          }
        }

        "must throw if contractorName is missing" in {
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
              SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
            )
            .success
            .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()

          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("contractorName missing for userId=")
          }
        }

        "must throw if employerReference is missing" in {
          val incompleteUa = userAnswersWithCisId
            .set(ReturnTypePage, MonthlyNilReturn)
            .success
            .value
            .set(EnterYourEmailAddressPage, email)
            .success
            .value
            .set(ContractorNamePage, contractorName)
            .success
            .value
            .set(DateConfirmPaymentsPage, periodEnd)
            .success
            .value
            .set(
              SubmissionDetailsPage,
              SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
            )
            .success
            .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa), hasEmployeeRef = false).build()
          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("employerReference missing for userId=")
          }
        }

        "must throw if taxPeriodEnd is missing" in {
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
              SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
            )
            .success
            .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()
          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("[SubmissionSuccess] taxPeriodEnd missing from userAnswers")
          }
        }

        "must throw if submissionDetails is missing" in {
          val incompleteUa = userAnswersWithCisId
            .set(ReturnTypePage, MonthlyNilReturn)
            .success
            .value
            .set(ContractorNamePage, contractorName)
            .success
            .value
            .set(DateConfirmPaymentsPage, periodEnd)
            .success
            .value
            .set(EnterYourEmailAddressPage, "test@test.com")
            .success
            .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()
          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("[SubmissionSuccess] submissionDetails missing from userAnswers")
          }
        }

        "must throw if returnTypePage is missing" in {

          val incompleteUa =
            userAnswersWithCisId
              .set(ContractorNamePage, contractorName)
              .success
              .value
              .set(DateConfirmPaymentsPage, periodEnd)
              .success
              .value
              .set(EnterYourEmailAddressPage, email)
              .success
              .value
              .set(
                SubmissionDetailsPage,
                SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
              )
              .success
              .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()

          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("ReturnTypePage missing from userAnswers")
          }
        }

        "must call monthlyReturnService and use returned email when EnterYourEmailAddressPage is missing" in {

          val fallbackEmail = "fallback@test.com"

          val uaWithoutEmail: UserAnswers = ua
            .remove(EnterYourEmailAddressPage)
            .success
            .value
            .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
            .success
            .value

          val mockService = mock(classOf[MonthlyReturnService])

          when(mockService.getSchemeEmail(any())(any()))
            .thenReturn(Future.successful(Some(fallbackEmail)))

          val app =
            applicationBuilder(userAnswers = Some(uaWithoutEmail))
              .overrides(
                bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
                bind[MonthlyReturnService].toInstance(mockService)
              )
              .build()

          val view = app.injector.instanceOf[SubmissionSuccessView]

          lazy val expectedHtml: String =
            view(
              reference = reference,
              periodEnd = periodEnd.format(dmyFmt),
              submittedTime = submittedTime,
              submittedDate = submittedDate,
              contractorName = contractorName,
              empRef = employerRef,
              email = fallbackEmail,
              submissionType = submissionType
            )(request, messages(app)).toString

          running(app) {
            val result = route(app, request).value

            status(result) mustBe OK
            contentAsString(result) mustBe expectedHtml
          }

          verify(mockService).getSchemeEmail(any())(any())
        }

      }
    }

    "agent" - {

      "onPageLoad" - {

        val userAnswersWithAgentClientData = ua
          .set(AgentClientDataPage, agentDate)
          .success
          .value

        val userAnswersWithReturnType = userAnswersWithAgentClientData
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value

        lazy val app: Application =
          applicationBuilder(userAnswers = Some(userAnswersWithReturnType), isAgent = true)
            .overrides(bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)))
            .build()

        "must return OK and render the expected view" in {
          running(app) {
            val result = route(app, request).value

            status(result) mustBe OK
            contentAsString(result) mustBe expectedHtml
          }
        }

        "must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in {

          val app = applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true).build()

          running(app) {

            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(
              result
            ).value mustEqual controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
          }
        }

        "must throw if contractorName is missing" in {
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
              SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
            )
            .success
            .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa), isAgent = true).build()
          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("contractorName missing for userId=")
          }
        }

        "must throw if employerReference is missing" in {
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
              SubmissionDetails(id = "123", status = "ACCEPTED", irMark = irMarkBase64, submittedAt = Instant.now)
            )
            .success
            .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa), isAgent = true, hasAgentRef = false).build()
          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("employerReference missing for userId=")
          }
        }

      }
    }

  }

}
