/*
 * Copyright 2026 HM Revenue & Customs
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
import connectors.ConstructionIndustrySchemeConnector
import controllers.actions.*
import models.{ReturnType, UserAnswers}
import models.monthlyreturns.Declaration.Confirmed
import models.monthlyreturns.*
import models.requests.{DataRequest, GetMonthlyReturnCompleteRequest}
import models.submission.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import pages.submission.*
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.guice.GuiceApplicationBuilder
import services.{FakeFormpRdsReconcileService, FormpRdsReconcileService}
import repositories.SessionRepository
import services.guard.SubmissionSuccessfulServiceGuard
import services.submission.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.Future

class StandardReturnSubmissionJourneySpec extends SpecBase with MockitoSugar {

  "ID-scoped standard return submission journey" - {

    "must complete a standard return from Check Your Answers to submission confirmation" in {

      val flowId = UUID.randomUUID()
      val id     = flowId.toString

      val otherFlowId = UUID.randomUUID()
      val otherId     = otherFlowId.toString

      val periodEnd    = LocalDate.of(2024, 3, 5)
      val otherPeriod  = LocalDate.of(2024, 4, 5)
      val submissionId = "scoped-sub-123"
      val irMark       = "Pyy1LRJh053AE+nuyp0GJR7oESw="

      val subcontractor = SelectedSubcontractor(
        id = 1001L,
        name = "Tyne Test Ltd",
        totalPaymentsMade = Some(BigDecimal("1200")),
        costOfMaterials = Some(BigDecimal("500")),
        totalTaxDeducted = Some(BigDecimal("240"))
      )

      val otherSubcontractor = SelectedSubcontractor(
        id = 2001L,
        name = "Other Test Ltd",
        totalPaymentsMade = Some(BigDecimal("5000")),
        costOfMaterials = Some(BigDecimal("1000")),
        totalTaxDeducted = Some(BigDecimal("1000"))
      )

      val userAnswers = emptyUserAnswers
        // Multi-tab: Current standard return journey
        .setOrException(CisIdPage(id), "2")
        .setOrException(ReturnTypePage(id), ReturnType.MonthlyStandardReturn)
        .setOrException(DateConfirmPaymentsPage(id), periodEnd)
        .setOrException(ContractorNamePage(id), "Scoped Contractor")
        .setOrException(SelectedSubcontractorForFlowPage(id, 1), subcontractor)
        .setOrException(OriginalSubcontractorCountPage(id), 1)
        .setOrException(AllSubcontractorDetailsAdded(id), true)
        .setOrException(PaymentDetailsConfirmationPage(id), true)
        .setOrException(EmploymentStatusDeclarationPage(id), true)
        .setOrException(VerifiedStatusDeclarationPage(id), true)
        .setOrException(SubmitInactivityRequestPage(id), true)
        .setOrException(ConfirmationByEmailPage(id), true)
        .setOrException(EnterYourEmailAddressPage(id), "scoped@example.com")
        .setOrException(DeclarationPage(id), Set(Confirmed))

        // Multi-tab: Another open standard return journey
        .setOrException(CisIdPage(otherId), "3")
        .setOrException(ReturnTypePage(otherId), ReturnType.MonthlyStandardReturn)
        .setOrException(DateConfirmPaymentsPage(otherId), otherPeriod)
        .setOrException(
          SelectedSubcontractorForFlowPage(otherId, 1),
          otherSubcontractor
        )

        // Legacy answers must not be used by the scoped journey
        .setOrException(CisIdPage, "legacy-cis-id")
        .setOrException(ReturnTypePage, ReturnType.MonthlyNilReturn)
        .setOrException(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .setOrException(EnterYourEmailAddressPage, "legacy@example.com")

      val acceptedDetails = SubmissionDetails(
        id = submissionId,
        status = "ACCEPTED",
        irMark = irMark,
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )

      val acceptedAnswers = userAnswers
        .setOrException(SubmissionDetailsPage(id), acceptedDetails)

      val submittedAnswers = acceptedAnswers
        .setOrException(
          SubmissionDetailsPage(id),
          acceptedDetails.copy(status = "SUBMITTED")
        )

      val chrisResponse = ChrisSubmissionResponse(
        submissionId = submissionId,
        status = "ACCEPTED",
        hmrcMarkGenerated = irMark,
        correlationId = Some("CID123"),
        responseEndPoint = None,
        gatewayTimestamp = Some("2025-01-01T00:00:00"),
        error = None
      )

      val monthlyReturnResponse = GetAllMonthlyReturnDetailsResponse(
        scheme = Seq(
          ContractorScheme(
            schemeId = 1,
            instanceId = "2",
            accountsOfficeReference = "123PA12345678",
            taxOfficeNumber = "123",
            taxOfficeReference = "AB456",
            name = Some("Scoped Contractor")
          )
        ),
        monthlyReturn = Seq.empty,
        subcontractors = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val mockConnector         = mock[ConstructionIndustrySchemeConnector]
      val mockSessionRepository = mock[SessionRepository]
      val mockSubmissionService = mock[SubmissionService]
      val mockGuard             = mock[SubmissionSuccessfulServiceGuard]

      when(mockSessionRepository.get(any[String]))
        .thenReturn(Future.successful(Some(userAnswers)))

      when(mockSessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      when(
        mockConnector.updateMonthlyReturn(
          any[UpdateMonthlyReturnRequest]
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        mockSubmissionService.isAlreadySubmitted(
          any[UserAnswers],
          eqTo(id)
        )
      ).thenReturn(false)

      when(
        mockSubmissionService.getOrCreateSubmissionForChris(
          any[UserAnswers],
          eqTo(id)
        )(using any[HeaderCarrier])
      ).thenReturn(
        Future.successful((submissionId, userAnswers, false))
      )

      when(
        mockSubmissionService.submitToChrisAndPersist(
          eqTo(submissionId),
          any[UserAnswers],
          eqTo(id),
          any[Boolean],
          eqTo(false)
        )(using any[HeaderCarrier])
      ).thenReturn(Future.successful(chrisResponse))

      when(
        mockSubmissionService.updateSubmissionFromChrisResponse(
          eqTo(submissionId),
          any[UserAnswers],
          eqTo(id),
          eqTo(chrisResponse)
        )(any[DataRequest[AnyContent]], any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(mockSubmissionService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockSubmissionService.checkAndUpdateSubmissionStatusIfAllowed(
          any[UserAnswers],
          eqTo(id)
        )(using any[HeaderCarrier], any[DataRequest[AnyContent]])
      ).thenReturn(
        Future.successful(PollDecision.Polled("SUBMITTED"))
      )

      when(
        mockSubmissionService.sendSuccessEmail(
          any[UserAnswers],
          eqTo(id),
          any[String]
        )(using any[HeaderCarrier])
      ).thenReturn(Future.successful(submittedAnswers))

      when(mockGuard.check(any[UserAnswers], eqTo(id)))
        .thenReturn(true)

      when(
        mockConnector.getMonthlyReturnComplete(
          any[GetMonthlyReturnCompleteRequest]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(monthlyReturnResponse))

      val application = new GuiceApplicationBuilder()
        .configure(
          "play.http.router"           -> "app.Routes",
          "features.multi-tab-enabled" -> true
        )
        .overrides(
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to(
            new FakeIdentifierAction(false, true, true)(parsers)
          ),
          bind[IdentifierAction]
            .qualifiedWith("AgentIdentifier")
            .to(new FakeIdentifierAction(true, true, false, Some("agentCode"))(parsers)),
          bind[IdentifierAction]
            .qualifiedWith("ContractorIdentifier")
            .to(new FakeIdentifierAction(false, false, true)(parsers)),
          bind[DataRetrievalAction].to[DataRetrievalActionImpl],
          bind[FormpRdsReconcileService].toInstance(new FakeFormpRdsReconcileService),
          bind[ConstructionIndustrySchemeConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[SubmissionService].toInstance(mockSubmissionService),
          bind[SubmissionSuccessfulServiceGuard].toInstance(mockGuard)
        )
        .build()

      running(application) {

        val cyaGetResult = route(
          application,
          FakeRequest(
            GET,
            routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
          )
        ).value

        status(cyaGetResult) mustBe OK

        val cyaBody = contentAsString(cyaGetResult)

        cyaBody must include("scoped@example.com")
        cyaBody must not include "legacy@example.com"

        cyaBody must include(
          routes.CheckYourAnswersController.onSubmitWithId(flowId).url
        )

        val cyaPostResult = route(
          application,
          FakeRequest(
            POST,
            routes.CheckYourAnswersController.onSubmitWithId(flowId).url
          )
        ).value

        status(cyaPostResult) mustBe SEE_OTHER

        val sendingUrl = redirectLocation(cyaPostResult).value

        sendingUrl mustBe
          routes.SubmissionSendingController.onPageLoadWithId(flowId).url

        val expectedUpdateRequest = UpdateMonthlyReturnRequest
          .fromUserAnswers(userAnswers, id)
          .toOption
          .value

        verify(mockConnector)
          .updateMonthlyReturn(
            eqTo(expectedUpdateRequest)
          )(any[HeaderCarrier])

        verify(mockSubmissionService)
          .isAlreadySubmitted(
            any[UserAnswers],
            eqTo(id)
          )

        val sendingResult = route(
          application,
          FakeRequest(GET, sendingUrl)
        ).value

        status(sendingResult) mustBe SEE_OTHER

        val pollingUrl = redirectLocation(sendingResult).value

        pollingUrl mustBe
          routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url

        val submissionAnswersCaptor =
          ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockSubmissionService)
          .getOrCreateSubmissionForChris(
            submissionAnswersCaptor.capture(),
            eqTo(id)
          )(using any[HeaderCarrier])

        val answersForSubmission = submissionAnswersCaptor.getValue

        answersForSubmission.get(ReturnTypePage(id)) mustBe
          Some(ReturnType.MonthlyStandardReturn)

        answersForSubmission.get(CisIdPage(id)) mustBe Some("2")

        answersForSubmission.get(DateConfirmPaymentsPage(id)) mustBe
          Some(periodEnd)

        answersForSubmission.get(
          SelectedSubcontractorForFlowPage(id, 1)
        ) mustBe Some(subcontractor)

        answersForSubmission.get(
          SelectedSubcontractorForFlowPage(otherId, 1)
        ) mustBe Some(otherSubcontractor)

        verify(mockSubmissionService)
          .submitToChrisAndPersist(
            eqTo(submissionId),
            eqTo(userAnswers),
            eqTo(id),
            any[Boolean],
            eqTo(false)
          )(using any[HeaderCarrier])

        verify(mockSubmissionService)
          .updateSubmissionFromChrisResponse(
            eqTo(submissionId),
            eqTo(userAnswers),
            eqTo(id),
            eqTo(chrisResponse)
          )(any[DataRequest[AnyContent]], any[HeaderCarrier])

        when(mockSessionRepository.get(any[String]))
          .thenReturn(Future.successful(Some(acceptedAnswers)))

        val pollingResult = route(
          application,
          FakeRequest(GET, pollingUrl)
        ).value

        status(pollingResult) mustBe SEE_OTHER

        val confirmationUrl = redirectLocation(pollingResult).value

        confirmationUrl mustBe
          routes.SubmissionSuccessController.onPageLoadWithId(flowId).url

        verify(mockSubmissionService)
          .checkAndUpdateSubmissionStatusIfAllowed(
            eqTo(acceptedAnswers),
            eqTo(id)
          )(using any[HeaderCarrier], any[DataRequest[AnyContent]])

        verify(mockSubmissionService)
          .sendSuccessEmail(
            eqTo(acceptedAnswers),
            eqTo(id),
            any[String]
          )(using any[HeaderCarrier])

        when(mockSessionRepository.get(any[String]))
          .thenReturn(Future.successful(Some(submittedAnswers)))

        val confirmationResult = route(
          application,
          FakeRequest(GET, confirmationUrl)
        ).value

        status(confirmationResult) mustBe OK

        val confirmationBody = contentAsString(confirmationResult)

        confirmationBody must include("March 2024")
        confirmationBody must include("Scoped Contractor")
        confirmationBody must include("scoped@example.com")
        confirmationBody must not include "legacy@example.com"

        confirmationBody must include(
          routes.ManageCisReturnController.onExitWithId(flowId).url
        )

        verify(mockGuard)
          .check(
            eqTo(submittedAnswers),
            eqTo(id)
          )

        val completedRequestCaptor =
          ArgumentCaptor.forClass(classOf[GetMonthlyReturnCompleteRequest])

        verify(mockConnector)
          .getMonthlyReturnComplete(
            completedRequestCaptor.capture()
          )(any[HeaderCarrier])

        val completedRequest = completedRequestCaptor.getValue

        completedRequest.instanceId mustBe "2"
        completedRequest.taxYear mustBe 2024
        completedRequest.taxMonth mustBe 3
        completedRequest.amendment mustBe "N"

        val savedAnswersCaptor =
          ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockSessionRepository, atLeastOnce())
          .set(savedAnswersCaptor.capture())

        val savedAnswers = savedAnswersCaptor.getValue

        savedAnswers.get(
          SubmissionJourneyCompletedForFlowPage(id, "2024-03")
        ) mustBe Some(true)

        savedAnswers.get(
          SubmissionJourneyCompletedPage("2024-03")
        ) mustBe None

        savedAnswers.get(ReturnTypePage(id)) mustBe
          Some(ReturnType.MonthlyStandardReturn)

        savedAnswers.get(SubmissionDetailsPage(id)) mustBe
          Some(acceptedDetails.copy(status = "SUBMITTED"))

        savedAnswers.get(
          SelectedSubcontractorForFlowPage(id, 1)
        ) mustBe Some(subcontractor)

        savedAnswers.get(
          SubmissionJourneyCompletedForFlowPage(otherId, "2024-04")
        ) mustBe None

        savedAnswers.get(
          SubmissionJourneyCompletedForFlowPage(otherId, "2024-03")
        ) mustBe None

        savedAnswers.get(
          SelectedSubcontractorForFlowPage(otherId, 1)
        ) mustBe Some(otherSubcontractor)

        savedAnswers.get(DateConfirmPaymentsPage(otherId)) mustBe
          Some(otherPeriod)

        savedAnswers.get(ReturnTypePage(otherId)) mustBe
          Some(ReturnType.MonthlyStandardReturn)

        savedAnswers.get(CisIdPage(otherId)) mustBe Some("3")

        savedAnswers.get(ReturnTypePage) mustBe
          Some(ReturnType.MonthlyNilReturn)

        savedAnswers.get(CisIdPage) mustBe Some("legacy-cis-id")

        savedAnswers.get(DateConfirmPaymentsPage) mustBe
          Some(LocalDate.of(2025, 8, 5))

        savedAnswers.get(EnterYourEmailAddressPage) mustBe
          Some("legacy@example.com")

        verify(mockSubmissionService, never())
          .isAlreadySubmitted(any[UserAnswers])

        verify(mockSubmissionService, never())
          .getOrCreateSubmissionForChris(
            any[UserAnswers]
          )(using any[HeaderCarrier])

        verify(mockSubmissionService, never())
          .sendSuccessEmail(
            any[UserAnswers],
            any[String]
          )(using any[HeaderCarrier])
      }
    }
  }
}
