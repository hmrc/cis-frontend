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
import controllers.monthlyreturns
import models.agent.AgentClientData
import models.{ReturnType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{ContractorNamePage, DateConfirmPaymentsPage, EnterYourEmailAddressPage, ReturnTypePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import views.html.monthlyreturns.SubmittedNoReceiptView

import java.time.format.DateTimeFormatter
import java.time.*
import java.util.Locale
import scala.concurrent.Future

class SubmittedNoReceiptControllerSpec extends SpecBase {

  val email: String              = "test@test.com"
  val periodEnd: LocalDate       = LocalDate.of(2018, 3, 5)
  val fixedInstant: Instant      = Instant.parse("2017-01-06T08:46:00Z")
  val contractorName: String     = "PAL 355 Scheme"
  val employerRef: String        = "taxOfficeNumber/taxOfficeReference"
  val submissionType: ReturnType = ReturnType.MonthlyNilReturn
  val cisId                      = "1"

  private val dmyFmt  = DateTimeFormatter.ofPattern("MMMM uuuu").withLocale(Locale.UK)
  private val timeFmt = DateTimeFormatter.ofPattern("h:mma").withLocale(Locale.UK)
  private val london  = ZoneId.of("Europe/London")

  protected lazy val ukNow: ZonedDateTime =
    ZonedDateTime.ofInstant(fixedInstant, london)

  protected lazy val submittedTime: String =
    ukNow.format(timeFmt).toLowerCase

  protected lazy val submittedDate: String =
    ukNow.format(DateTimeFormatter.ofPattern("d MMMM uuuu"))

  val baseUa: UserAnswers =
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
      .set(ReturnTypePage, submissionType)
      .success
      .value

  lazy val request =
    FakeRequest(GET, routes.SubmittedNoReceiptController.onPageLoad.url)

  "SubmittedNoReceiptController" - {

    "contractor" - {

      "onPageLoad" - {

        "must return OK and render the expected view" in {

          val app =
            applicationBuilder(userAnswers = Some(baseUa))
              .overrides(bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)))
              .build()

          val view = app.injector.instanceOf[SubmittedNoReceiptView]

          val expectedHtml =
            view(
              periodEnd = periodEnd.format(dmyFmt),
              submittedTime = submittedTime,
              submittedDate = submittedDate,
              contractorName = contractorName,
              empRef = employerRef,
              email = email,
              submissionType = submissionType,
              cisId = cisId
            )(request, applicationConfig, messages(app)).toString

          running(app) {
            val result = route(app, request).value
            status(result) mustBe OK
            contentAsString(result) mustBe expectedHtml
          }
        }

        "must redirect to Unauthorised Organisation Affinity if cisId is missing from UserAnswers" in {

          val app = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(app) {
            val result = route(app, request).value
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe
              controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
          }
        }

        "must throw if contractorName missing" in {

          val incompleteUa =
            userAnswersWithCisId
              .set(DateConfirmPaymentsPage, periodEnd)
              .success
              .value
              .set(ReturnTypePage, submissionType)
              .success
              .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()

          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("contractorName missing")
          }
        }

        "must throw if taxPeriodEnd is missing" in {

          val incompleteUa =
            userAnswersWithCisId
              .set(ContractorNamePage, contractorName)
              .success
              .value
              .set(EnterYourEmailAddressPage, email)
              .success
              .value
              .set(ReturnTypePage, submissionType)
              .success
              .value

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()

          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("taxPeriodEnd missing")
          }
        }

        "must throw if employerReference is missing" in {

          val app = applicationBuilder(userAnswers = Some(baseUa), hasEmployeeRef = false).build()
          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("employerReference missing for userId=")
          }

        }

        "must call monthlyReturnService and use returned email when EnterYourEmailAddressPage is missing" in {

          val fallbackEmail = "fallback@test.com"

          val uaWithoutEmail =
            userAnswersWithCisId
              .set(ContractorNamePage, contractorName)
              .success
              .value
              .set(DateConfirmPaymentsPage, periodEnd)
              .success
              .value
              .set(ReturnTypePage, submissionType)
              .success
              .value

          val mockService = mock[MonthlyReturnService]

          when(mockService.getSchemeEmail(any())(any()))
            .thenReturn(Future.successful(Some(fallbackEmail)))

          val app =
            applicationBuilder(userAnswers = Some(uaWithoutEmail))
              .overrides(
                bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
                bind[MonthlyReturnService].toInstance(mockService)
              )
              .build()

          val view = app.injector.instanceOf[SubmittedNoReceiptView]

          val expectedHtml =
            view(
              periodEnd = periodEnd.format(dmyFmt),
              submittedTime = submittedTime,
              submittedDate = submittedDate,
              contractorName = contractorName,
              empRef = employerRef,
              email = fallbackEmail,
              submissionType = submissionType,
              cisId = cisId
            )(request, applicationConfig, messages(app)).toString

          running(app) {
            val result = route(app, request).value

            status(result) mustBe OK
            contentAsString(result) mustBe expectedHtml
          }

          verify(mockService).getSchemeEmail(any())(any())
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

          val app = applicationBuilder(userAnswers = Some(incompleteUa)).build()

          running(app) {
            val thrown = intercept[IllegalStateException] {
              await(route(app, request).get)
            }
            thrown.getMessage must include("ReturnTypePage missing from userAnswers")
          }
        }

      }

      "agent" - {

        "onPageLoad" - {

          "must return OK and render the expected view" in {

            val app =
              applicationBuilder(userAnswers = Some(baseUa))
                .overrides(bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)))
                .build()

            val view = app.injector.instanceOf[SubmittedNoReceiptView]

            val expectedHtml =
              view(
                periodEnd = periodEnd.format(dmyFmt),
                submittedTime = submittedTime,
                submittedDate = submittedDate,
                contractorName = contractorName,
                empRef = employerRef,
                email = email,
                submissionType = submissionType,
                cisId = cisId
              )(request, applicationConfig, messages(app)).toString

            running(app) {
              val result = route(app, request).value
              status(result) mustBe OK
              contentAsString(result) mustBe expectedHtml
            }
          }

          "must redirect to Unauthorised Organisation Affinity if cisId is missing from UserAnswers" in {

            val app = applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true).build()

            running(app) {
              val result = route(app, request).value
              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe
                controllers.routes.UnauthorisedAgentAffinityController.onPageLoad().url
            }
          }

          "must throw if contractorName missing" in {

            val incompleteUa =
              userAnswersWithCisId
                .set(DateConfirmPaymentsPage, periodEnd)
                .success
                .value
                .set(ReturnTypePage, submissionType)
                .success
                .value

            val app = applicationBuilder(userAnswers = Some(incompleteUa), isAgent = true).build()

            running(app) {
              val thrown = intercept[IllegalStateException] {
                await(route(app, request).get)
              }
              thrown.getMessage must include("contractorName missing")
            }
          }

          "must throw if employerReference is missing" in {

            lazy val agentDateWithoutTaxRefTaxNumber: AgentClientData =
              AgentClientData("CLIENT-123", "", "taxOfficeReference", Some("PAL 355 Scheme"))

            val incompleteUa = userAnswersWithCisId
              .set(AgentClientDataPage, agentDateWithoutTaxRefTaxNumber)
              .success
              .value
              .set(DateConfirmPaymentsPage, periodEnd)
              .success
              .value

            val app =
              applicationBuilder(userAnswers = Some(incompleteUa), hasEmployeeRef = false, isAgent = true).build()
            running(app) {
              val thrown = intercept[IllegalStateException] {
                await(route(app, request).get)
              }
              thrown.getMessage must include("employerReference missing for userId=")
            }

          }

          "must call monthlyReturnService and use returned email when EnterYourEmailAddressPage is missing" in {

            val fallbackEmail = "fallback@test.com"

            lazy val agentDateWithoutTaxRefTaxNumber: AgentClientData =
              AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some("PAL 355 Scheme"))

            val uaWithoutEmail =
              userAnswersWithCisId
                .set(AgentClientDataPage, agentDateWithoutTaxRefTaxNumber)
                .success
                .value
                .set(ContractorNamePage, contractorName)
                .success
                .value
                .set(DateConfirmPaymentsPage, periodEnd)
                .success
                .value
                .set(ReturnTypePage, submissionType)
                .success
                .value

            val mockService = mock[MonthlyReturnService]

            when(mockService.getSchemeEmail(any())(any()))
              .thenReturn(Future.successful(Some(fallbackEmail)))

            val app =
              applicationBuilder(userAnswers = Some(uaWithoutEmail), isAgent = true)
                .overrides(
                  bind[Clock].toInstance(Clock.fixed(fixedInstant, ZoneOffset.UTC)),
                  bind[MonthlyReturnService].toInstance(mockService)
                )
                .build()

            val view = app.injector.instanceOf[SubmittedNoReceiptView]

            val expectedHtml =
              view(
                periodEnd = periodEnd.format(dmyFmt),
                submittedTime = submittedTime,
                submittedDate = submittedDate,
                contractorName = contractorName,
                empRef = employerRef,
                email = fallbackEmail,
                submissionType = submissionType,
                cisId = cisId
              )(request, applicationConfig, messages(app)).toString

            running(app) {
              val result = route(app, request).value

              status(result) mustBe OK
              contentAsString(result) mustBe expectedHtml
            }

            verify(mockService).getSchemeEmail(any())(any())
          }

          "must throw if returnTypePage is missing" in {

            lazy val agentDateWithoutTaxRefTaxNumber: AgentClientData =
              AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some("PAL 355 Scheme"))

            val incompleteUa =
              userAnswersWithCisId
                .set(AgentClientDataPage, agentDateWithoutTaxRefTaxNumber)
                .success
                .value
                .set(DateConfirmPaymentsPage, periodEnd)
                .success
                .value
                .set(EnterYourEmailAddressPage, email)
                .success
                .value

            val app = applicationBuilder(userAnswers = Some(incompleteUa), isAgent = true).build()

            running(app) {
              val thrown = intercept[IllegalStateException] {
                await(route(app, request).get)
              }
              thrown.getMessage must include("ReturnTypePage missing from userAnswers")
            }
          }

        }

      }

    }
  }
}
