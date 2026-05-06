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

package controllers.amend

import base.SpecBase
import controllers.actions.{FakeDataRetrievalAction, FakeIdentifierAction}
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.UserAnswers
import models.agent.AgentClientData
import models.amend.{AmendmentDetails, CreateAmendedMonthlyReturnRequest}
import models.monthlyreturns.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.amend.{AmendmentDetailsPage, ConfirmAmendmentPage}
import pages.monthlyreturns.CisIdPage
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.amend.ConfirmAmendmentView

import scala.concurrent.{ExecutionContext, Future}

class ConfirmAmendmentControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockSessionRepository         = mock[SessionRepository]
  private val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]
  private val mockMonthlyReturnService      = mock[MonthlyReturnService]
  private val mockView                      = mock[ConfirmAmendmentView]

  private val queryParams = ContinueReturnJourneyQueryParams(
    instanceId = "CIS-123",
    taxYear = 2025,
    taxMonth = 3
  )

  private def taxpayer(cisId: String = "CIS-123"): CisTaxpayer =
    CisTaxpayer(
      uniqueId = cisId,
      taxOfficeNumber = "111",
      taxOfficeRef = "AB123",
      aoDistrict = None,
      aoPayType = None,
      aoCheckCode = None,
      aoReference = None,
      validBusinessAddr = None,
      correlation = None,
      ggAgentId = None,
      employerName1 = Some("Test Ltd"),
      employerName2 = None,
      agentOwnRef = None,
      schemeName = None,
      utr = None,
      enrolledSig = None
    )

  private def amendmentDetails: AmendmentDetails =
    AmendmentDetails(
      instanceId = "CIS-123",
      taxYear = 2025,
      taxMonth = 3,
      returnType = MonthlyStandardReturn,
      acceptedTime = Some("2025-04-05T12:00:00Z")
    )

  private def monthlyReturnPayload(
    taxYear: Int = 2025,
    taxMonth: Int = 3,
    nilReturnIndicator: Option[String] = Some("N"),
    acceptedTime: Option[String] = Some("2025-04-05T12:00:00Z")
  ): GetAllMonthlyReturnDetailsResponse =
    GetAllMonthlyReturnDetailsResponse(
      scheme = Nil,
      monthlyReturn = Seq(
        MonthlyReturn(
          monthlyReturnId = 101,
          taxYear = taxYear,
          taxMonth = taxMonth,
          nilReturnIndicator = nilReturnIndicator
        )
      ),
      subcontractors = Nil,
      monthlyReturnItems = Nil,
      submission = Seq(
        Submission(
          submissionId = 1,
          submissionType = "MONTHLY_RETURN",
          activeObjectId = Some(101),
          status = None,
          hmrcMarkGenerated = None,
          hmrcMarkGgis = None,
          emailRecipient = None,
          acceptedTime = acceptedTime,
          createDate = None,
          lastUpdate = None,
          schemeId = 1,
          agentId = None,
          l_Migrated = None,
          submissionRequestDate = None,
          govTalkErrorCode = None,
          govTalkErrorType = None,
          govTalkErrorMessage = None
        )
      )
    )

  private def agentClientData: AgentClientData =
    AgentClientData(
      uniqueId = "CIS-123",
      taxOfficeNumber = "163",
      taxOfficeReference = "AB0063",
      schemeName = Some("ABC Ltd")
    )

  private def mockOrgAccess(): Unit =
    when(mockMonthlyReturnService.getCisTaxpayer(any[HeaderCarrier]))
      .thenReturn(Future.successful(taxpayer("CIS-123")))

  private def mockOrgAccessDenied(): Unit =
    when(mockMonthlyReturnService.getCisTaxpayer(any[HeaderCarrier]))
      .thenReturn(Future.successful(taxpayer("DIFFERENT-CIS-ID")))

  private def mockOrgAccessFails(): Unit =
    when(mockMonthlyReturnService.getCisTaxpayer(any[HeaderCarrier]))
      .thenReturn(Future.failed(new RuntimeException("taxpayer lookup failed")))

  private def mockRetrieveMonthlyReturn(
    payload: GetAllMonthlyReturnDetailsResponse = monthlyReturnPayload()
  ): Unit =
    when(
      mockMonthlyReturnService.retrieveMonthlyReturnForEditDetails(
        any[String],
        any[Int],
        any[Int]
      )(any[HeaderCarrier])
    ).thenReturn(Future.successful(payload))

  private def mockRetrieveMonthlyReturnFails(): Unit =
    when(
      mockMonthlyReturnService.retrieveMonthlyReturnForEditDetails(
        any[String],
        any[Int],
        any[Int]
      )(any[HeaderCarrier])
    ).thenReturn(Future.failed(new RuntimeException("retrieve failed")))

  private def controller(
    userAnswers: Option[UserAnswers] = Some(UserAnswers("test-user")),
    isAgent: Boolean = false
  ): ConfirmAmendmentController = {
    val messagesApi = app.injector.instanceOf[MessagesApi]
    val mcc         = app.injector.instanceOf[MessagesControllerComponents]
    val bodyParsers = app.injector.instanceOf[PlayBodyParsers]

    when(mockView()(any(), any()))
      .thenReturn(play.twirl.api.HtmlFormat.empty)

    new ConfirmAmendmentController(
      messagesApi = messagesApi,
      identify = new FakeIdentifierAction(
        isAgent = isAgent,
        hasAgentRef = true,
        hasEmployeeRef = true
      )(bodyParsers),
      getData = new FakeDataRetrievalAction(userAnswers),
      sessionRepository = mockSessionRepository,
      amendMonthlyReturnService = mockAmendMonthlyReturnService,
      monthlyReturnService = mockMonthlyReturnService,
      controllerComponents = mcc,
      view = mockView
    )
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(
      mockSessionRepository,
      mockAmendMonthlyReturnService,
      mockMonthlyReturnService,
      mockView
    )

    when(mockView()(any(), any()))
      .thenReturn(play.twirl.api.HtmlFormat.empty)
  }

  "ConfirmAmendmentController" - {

    "onPageLoad" - {

      "must return OK and store amendment details when organisation user is authorised" in {
        mockOrgAccess()
        mockRetrieveMonthlyReturn()

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller().onPageLoad(queryParams)(request)

        status(result) mustBe OK

        val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture())

        val savedUa = uaCaptor.getValue

        savedUa.get(CisIdPage) mustBe Some("CIS-123")
        savedUa.get(AmendmentDetailsPage) mustBe Some(amendmentDetails)

        verify(mockMonthlyReturnService).getCisTaxpayer(any[HeaderCarrier])
        verify(mockMonthlyReturnService).retrieveMonthlyReturnForEditDetails(
          any[String],
          any[Int],
          any[Int]
        )(any[HeaderCarrier])
      }

      "must store MonthlyNilReturn when nilReturnIndicator is Y" in {
        mockOrgAccess()

        mockRetrieveMonthlyReturn(
          monthlyReturnPayload(
            nilReturnIndicator = Some("Y"),
            acceptedTime = None
          )
        )

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller().onPageLoad(queryParams)(request)

        status(result) mustBe OK

        val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture())

        val savedDetails = uaCaptor.getValue.get(AmendmentDetailsPage).value

        savedDetails.returnType mustBe MonthlyNilReturn
        savedDetails.acceptedTime mustBe None
      }

      "must redirect to Journey Recovery when organisation user is not authorised" in {
        mockOrgAccessDenied()

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller().onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockMonthlyReturnService).getCisTaxpayer(any[HeaderCarrier])
        verify(mockMonthlyReturnService, times(0)).retrieveMonthlyReturnForEditDetails(
          any[String],
          any[Int],
          any[Int]
        )(any[HeaderCarrier])
      }

      "must redirect to Journey Recovery when organisation validation fails" in {
        mockOrgAccessFails()

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller().onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery when retrieve amendment details fails" in {
        mockOrgAccess()
        mockRetrieveMonthlyReturnFails()

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller().onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery when no matching monthly return is found" in {
        mockOrgAccess()

        mockRetrieveMonthlyReturn(
          monthlyReturnPayload(
            taxYear = 2024,
            taxMonth = 12
          )
        )

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller().onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must allow agent when agent client exists and hasClient returns true" in {
        when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(agentClientData)))

        when(mockMonthlyReturnService.hasClient(any[String], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(true))

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        mockRetrieveMonthlyReturn()

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller(isAgent = true).onPageLoad(queryParams)(request)

        status(result) mustBe OK

        verify(mockMonthlyReturnService).getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMonthlyReturnService).hasClient(any[String], any[String])(any[HeaderCarrier])
      }

      "must redirect to Journey Recovery when agent client data is missing" in {
        when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller(isAgent = true).onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery when agent no longer has access to client" in {
        when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(agentClientData)))

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(mockMonthlyReturnService.hasClient(any[String], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller(isAgent = true).onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery when agent validation fails" in {
        when(mockMonthlyReturnService.getAgentClient(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException("agent lookup failed")))

        val request = FakeRequest(GET, routes.ConfirmAmendmentController.onPageLoad(queryParams).url)

        val result = controller(isAgent = true).onPageLoad(queryParams)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "onSubmit" - {

      "must create amended monthly return, store confirmation, and redirect when organisation user is authorised" in {
        mockOrgAccess()

        val userAnswers = UserAnswers("test-user")
          .set(AmendmentDetailsPage, amendmentDetails)
          .get

        when(
          mockAmendMonthlyReturnService.createAmendedMonthlyReturn(
            any[CreateAmendedMonthlyReturnRequest]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        val request = FakeRequest(POST, routes.ConfirmAmendmentController.onSubmit().url)
          .withFormUrlEncodedBody()

        val result = controller(Some(userAnswers)).onSubmit(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          routes.ConfirmAmendmentController.onPageLoad(queryParams).url

        val requestCaptor = ArgumentCaptor.forClass(classOf[CreateAmendedMonthlyReturnRequest])

        verify(mockAmendMonthlyReturnService).createAmendedMonthlyReturn(requestCaptor.capture())(
          any[HeaderCarrier]
        )

        requestCaptor.getValue mustBe CreateAmendedMonthlyReturnRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 3,
          version = 0
        )

        val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture())

        uaCaptor.getValue.get(ConfirmAmendmentPage) mustBe Some(true)
      }

      "must redirect to Journey Recovery when AmendmentDetails are missing" in {
        val request = FakeRequest(POST, routes.ConfirmAmendmentController.onSubmit().url)
          .withFormUrlEncodedBody()

        val result = controller(Some(UserAnswers("test-user"))).onSubmit(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockAmendMonthlyReturnService)
      }

      "must redirect to Journey Recovery when organisation user is not authorised" in {
        mockOrgAccessDenied()

        val userAnswers = UserAnswers("test-user")
          .set(AmendmentDetailsPage, amendmentDetails)
          .get

        val request = FakeRequest(POST, routes.ConfirmAmendmentController.onSubmit().url)
          .withFormUrlEncodedBody()

        val result = controller(Some(userAnswers)).onSubmit(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockAmendMonthlyReturnService)
      }

      "must redirect to Journey Recovery when organisation validation fails" in {
        mockOrgAccessFails()

        val userAnswers = UserAnswers("test-user")
          .set(AmendmentDetailsPage, amendmentDetails)
          .get

        val request = FakeRequest(POST, routes.ConfirmAmendmentController.onSubmit().url)
          .withFormUrlEncodedBody()

        val result = controller(Some(userAnswers)).onSubmit(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery when create amended monthly return fails" in {
        mockOrgAccess()

        val userAnswers = UserAnswers("test-user")
          .set(AmendmentDetailsPage, amendmentDetails)
          .get

        when(
          mockAmendMonthlyReturnService.createAmendedMonthlyReturn(
            any[CreateAmendedMonthlyReturnRequest]
          )(any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("create failed")))

        val request = FakeRequest(POST, routes.ConfirmAmendmentController.onSubmit().url)
          .withFormUrlEncodedBody()

        val result = controller(Some(userAnswers)).onSubmit(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }

      "must redirect to Journey Recovery when session save fails" in {
        mockOrgAccess()

        val userAnswers = UserAnswers("test-user")
          .set(AmendmentDetailsPage, amendmentDetails)
          .get

        when(
          mockAmendMonthlyReturnService.createAmendedMonthlyReturn(
            any[CreateAmendedMonthlyReturnRequest]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.failed(new RuntimeException("session failed")))

        val request = FakeRequest(POST, routes.ConfirmAmendmentController.onSubmit().url)
          .withFormUrlEncodedBody()

        val result = controller(Some(userAnswers)).onSubmit(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
