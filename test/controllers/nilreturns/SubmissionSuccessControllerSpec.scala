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
import models.agent.AgentClientData
import models.submission.SubmissionDetails
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{ConfirmEmailAddressPage, ContractorNamePage, DateConfirmNilPaymentsPage}
import pages.submission.SubmissionDetailsPage
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import views.html.nilreturns.SubmissionSuccessView
import utils.IrMarkReferenceGenerator

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId, ZoneOffset, ZonedDateTime}

class SubmissionSuccessControllerSpec extends SpecBase {

  val email: String          = "test@test.com"
  val periodEnd: LocalDate   = LocalDate.of(2018, 3, 5)
  val fixedInstant: Instant  = Instant.parse("2017-01-06T08:46:00Z")
  val irMarkBase64: String   = "Pyy1LRJh053AE+nuyp0GJR7oESw="
  val reference: String      = IrMarkReferenceGenerator.fromBase64(irMarkBase64)
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

  val ua: UserAnswers =
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
      periodEnd = periodEnd.format(dmyFmt),
      submittedTime = submittedTime,
      submittedDate = submittedDate,
      contractorName = contractorName,
      empRef = employerRef,
      email = email
    )(request, messages(app)).toString

  lazy val agentDate: AgentClientData =
    AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some("PAL 355 Scheme"))

  "SubmissionSuccessController" - {

    "contractor" - {

      "onPageLoad" - {

        lazy val app: Application =
          applicationBuilder(userAnswers = Some(ua))
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

        "must throw if contractorName is missing" in {
          val incompleteUa = userAnswersWithCisId
            .set(DateConfirmNilPaymentsPage, periodEnd)
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
            .set(ContractorNamePage, contractorName)
            .success
            .value
            .set(DateConfirmNilPaymentsPage, periodEnd)
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
            .set(ContractorNamePage, contractorName)
            .success
            .value
            .set(ConfirmEmailAddressPage, "test@test.com")
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
            .set(ContractorNamePage, contractorName)
            .success
            .value
            .set(DateConfirmNilPaymentsPage, periodEnd)
            .success
            .value
            .set(ConfirmEmailAddressPage, "test@test.com")
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
      }
    }

    "agent" - {

      "onPageLoad" - {

        val userAnswersWithAgentClientData = ua
          .set(AgentClientDataPage, agentDate)
          .success
          .value

        lazy val app: Application =
          applicationBuilder(userAnswers = Some(userAnswersWithAgentClientData), isAgent = true)
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
            .set(AgentClientDataPage, agentDateWithoutTaxRefTaxNumber)
            .success
            .value
            .set(DateConfirmNilPaymentsPage, periodEnd)
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
