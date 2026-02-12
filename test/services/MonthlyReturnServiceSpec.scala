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

package services

import connectors.ConstructionIndustrySchemeConnector
import models.monthlyreturns.*
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, DeclarationPage, InactivityRequestPage, NilReturnStatusPage}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

class MonthlyReturnServiceSpec extends AnyWordSpec with ScalaFutures with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def newService(): (MonthlyReturnService, ConstructionIndustrySchemeConnector, SessionRepository) = {
    val connector   = mock(classOf[ConstructionIndustrySchemeConnector])
    val sessionRepo = mock(classOf[SessionRepository])
    val service     = new MonthlyReturnService(connector, sessionRepo)
    (service, connector, sessionRepo)
  }

  private def createMonthlyReturn(year: Int, month: Int, id: Long): MonthlyReturnDetails =
    MonthlyReturnDetails(
      monthlyReturnId = id,
      taxYear = year,
      taxMonth = month,
      nilReturnIndicator = None,
      decEmpStatusConsidered = None,
      decAllSubsVerified = None,
      decInformationCorrect = None,
      decNoMoreSubPayments = None,
      decNilReturnNoPayments = None,
      status = None,
      lastUpdate = None,
      amendment = None,
      supersededBy = None
    )

  private def createTaxpayer(
    id: String = "CIS-123",
    ton: String = "111",
    tor: String = "test111",
    name1: Option[String] = Some("TEST LTD")
  ): CisTaxpayer =
    CisTaxpayer(
      uniqueId = id,
      taxOfficeNumber = ton,
      taxOfficeRef = tor,
      aoDistrict = None,
      aoPayType = None,
      aoCheckCode = None,
      aoReference = None,
      validBusinessAddr = None,
      correlation = None,
      ggAgentId = None,
      employerName1 = name1,
      employerName2 = None,
      agentOwnRef = None,
      schemeName = None,
      utr = None,
      enrolledSig = None
    )

  "resolveAndStoreCisId" should {

    "return existing cisId from UserAnswers without calling BE" in {
      val (service, connector, sessionRepo) = newService()

      val existing    = "CIS-001"
      val emptyUa     = UserAnswers("test-user")
      val uaWithCisId = emptyUa.set(CisIdPage, existing).get

      val (cisId, savedUa) = service.resolveAndStoreCisId(uaWithCisId, false).futureValue
      cisId mustBe existing
      savedUa mustBe uaWithCisId

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepo)
    }

    "fetch taxpayer when missing, store cisId in session, and return updated UA" in {
      val (service, connector, sessionRepo) = newService()

      val emptyUa  = UserAnswers("test-user")
      val taxpayer = createTaxpayer()

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))
      when(sessionRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val (cisId, savedUa) = service.resolveAndStoreCisId(emptyUa, false).futureValue

      cisId mustBe "CIS-123"
      savedUa.get(CisIdPage) mustBe Some("CIS-123")

      val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepo).set(uaCaptor.capture())
      uaCaptor.getValue.get(CisIdPage) mustBe Some("CIS-123")

      verify(connector).getCisTaxpayer()(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "fail when BE returns empty uniqueId" in {
      val (service, connector, sessionRepo) = newService()
      val emptyUa                           = UserAnswers("test-user")

      val emptyTaxpayer = createTaxpayer(id = " ", name1 = None)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(emptyTaxpayer))

      val ex = intercept[RuntimeException] {
        service.resolveAndStoreCisId(emptyUa, false).futureValue
      }
      ex.getMessage must include("Empty cisId (uniqueId) returned from /cis/taxpayer")

      verify(connector).getCisTaxpayer()(any[HeaderCarrier])
      verifyNoInteractions(sessionRepo)
    }

    "fail when adding cisId to UserAnswers returns an error" in {
      val (service, connector, sessionRepo) = newService()

      val taxpayer = createTaxpayer()
      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val ua = mock(classOf[UserAnswers])
      when(ua.get(CisIdPage)).thenReturn(None)
      when(ua.set(CisIdPage, "CIS-123"))
        .thenReturn(Failure(new RuntimeException("UA set failed")))

      val ex = intercept[RuntimeException] {
        service.resolveAndStoreCisId(ua, false).futureValue
      }
      ex.getMessage must include("UA set failed")

      verifyNoInteractions(sessionRepo)
      verify(connector).getCisTaxpayer()(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "fail when cisId is missing and isAgent=true (agent journey requires instanceId)" in {
      val (service, connector, sessionRepo) = newService()

      val ua = UserAnswers("test-user")

      val ex = service.resolveAndStoreCisId(ua, isAgent = true).failed.futureValue
      ex mustBe a[RuntimeException]
      ex.getMessage must include("Missing cisId for agent journey")

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepo)
    }
  }

  "retrieveAllMonthlyReturns" should {

    "delegate to connector with the given cisId and return the payload" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      val payload = MonthlyReturnResponse(
        Seq(
          createMonthlyReturn(year = 2024, month = 4, id = 101L),
          createMonthlyReturn(year = 2024, month = 5, id = 102L)
        )
      )

      when(connector.retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(payload))

      val result = service.retrieveAllMonthlyReturns(cisId).futureValue
      result mustBe payload

      verify(connector).retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "propagate failures from the connector" in {
      val (service, connector, _) = newService()

      when(connector.retrieveMonthlyReturns(eqTo("CIS-ERR"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("upstream failed")))

      val ex = intercept[RuntimeException] {
        service.retrieveAllMonthlyReturns("CIS-ERR").futureValue
      }
      ex.getMessage must include("upstream failed")
    }
  }

  "isDuplicate" should {

    "return true when a monthly return with the same (year, month) exists" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      val fetchedMonthlyReturns = MonthlyReturnResponse(
        Seq(
          createMonthlyReturn(year = 2025, month = 9, id = 1L),
          createMonthlyReturn(year = 2024, month = 5, id = 2L)
        )
      )

      when(connector.retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(fetchedMonthlyReturns))

      service.isDuplicate(cisId, year = 2024, month = 5).futureValue mustBe true
    }

    "return false when no monthly return matches the (year, month)" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      val fetchedMonthlyReturns = MonthlyReturnResponse(
        Seq(
          createMonthlyReturn(year = 2024, month = 4, id = 1L),
          createMonthlyReturn(year = 2024, month = 6, id = 2L)
        )
      )

      when(connector.retrieveMonthlyReturns(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(fetchedMonthlyReturns))

      service.isDuplicate(cisId, year = 2024, month = 5).futureValue mustBe false
    }

    "propagate failures from the connector" in {
      val (service, connector, _) = newService()

      when(connector.retrieveMonthlyReturns(eqTo("CIS-123"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val ex = intercept[RuntimeException] {
        service.isDuplicate("CIS-123", year = 2024, month = 5).futureValue
      }
      ex.getMessage must include("boom")
    }
  }

  "retrieveMonthlyReturnForEditDetails" should {

    "delegate to connector and return the payload" in {
      val (service, connector, _) = newService()
      val instanceId              = "CIS-123"
      val taxMonth                = 10
      val taxYear                 = 2025

      val payload = GetAllMonthlyReturnDetailsResponse(
        scheme = Seq(
          ContractorScheme(
            schemeId = 1,
            instanceId = instanceId,
            accountsOfficeReference = "123PA12345678",
            taxOfficeNumber = "123",
            taxOfficeReference = "AB456"
          )
        ),
        monthlyReturn = Seq(
          MonthlyReturn(monthlyReturnId = 101, taxYear = taxYear, taxMonth = taxMonth)
        ),
        subcontractors = Seq(
          Subcontractor(
            subcontractorId = 1001,
            utr = None,
            pageVisited = None,
            partnerUtr = None,
            crn = None,
            firstName = Some("John"),
            nino = None,
            secondName = None,
            surname = Some("Doe"),
            partnershipTradingName = None,
            tradingName = Some("Test Subcontractor Ltd"),
            subcontractorType = None,
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = None,
            postCode = None,
            emailAddress = None,
            phoneNumber = None,
            mobilePhoneNumber = None,
            worksReferenceNumber = None,
            createDate = None,
            lastUpdate = None,
            subbieResourceRef = None,
            matched = None,
            autoVerified = None,
            verified = None,
            verificationNumber = None,
            taxTreatment = None,
            verificationDate = None,
            version = None,
            updatedTaxTreatment = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = None,
            displayName = Some("Test Subcontractor Ltd")
          )
        ),
        monthlyReturnItems = Seq(
          MonthlyReturnItem(
            monthlyReturnId = 101,
            monthlyReturnItemId = 2001,
            totalPayments = None,
            costOfMaterials = None,
            totalDeducted = None,
            unmatchedTaxRateIndicator = None,
            subcontractorId = Some(1001),
            subcontractorName = None,
            verificationNumber = None,
            itemResourceReference = None
          )
        ),
        submission = Seq(
          Submission(
            submissionId = 3001,
            submissionType = "MONTHLY_RETURN",
            activeObjectId = None,
            status = None,
            hmrcMarkGenerated = None,
            hmrcMarkGgis = None,
            emailRecipient = None,
            acceptedTime = None,
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

      when(
        connector.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(payload))

      val result = service.retrieveMonthlyReturnForEditDetails(instanceId, taxMonth, taxYear).futureValue
      result mustBe payload

      verify(connector).retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
        any[HeaderCarrier]
      )
      verifyNoMoreInteractions(connector)
    }

    "propagate failures from the connector" in {
      val (service, connector, _) = newService()

      when(connector.retrieveMonthlyReturnForEditDetails(eqTo("CIS-ERR"), eqTo(1), eqTo(2025))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("upstream failed")))

      val ex = intercept[RuntimeException] {
        service.retrieveMonthlyReturnForEditDetails("CIS-ERR", 1, 2025).futureValue
      }
      ex.getMessage must include("upstream failed")
    }
  }

  "getSchemeEmail" should {

    "delegate to connector and return the email when present" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      when(connector.getSchemeEmail(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some("test@example.com")))

      val result = service.getSchemeEmail(cisId).futureValue
      result mustBe Some("test@example.com")

      verify(connector).getSchemeEmail(eqTo(cisId))(any[HeaderCarrier])
    }

    "delegate to connector and return None when email is not present" in {
      val (service, connector, _) = newService()
      val cisId                   = "CIS-123"

      when(connector.getSchemeEmail(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = service.getSchemeEmail(cisId).futureValue
      result mustBe None

      verify(connector).getSchemeEmail(eqTo(cisId))(any[HeaderCarrier])
    }

    "propagate failures from the connector" in {
      val (service, connector, _) = newService()

      when(connector.getSchemeEmail(eqTo("CIS-ERR"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("upstream failed")))

      val ex = intercept[RuntimeException] {
        service.getSchemeEmail("CIS-ERR").futureValue
      }
      ex.getMessage must include("upstream failed")
    }
  }

  "createNilMonthlyReturn" should {

    "successfully create nil monthly return and mirror to session" in {
      val (service, connector, sessionRepo) = newService()

      val cisId    = "CIS-123"
      val taxYear  = 2024
      val taxMonth = 10
      val testDate = java.time.LocalDate.of(taxYear, taxMonth, 15)

      val userAnswers = UserAnswers("test-user")
        .set(CisIdPage, cisId)
        .get
        .set(DateConfirmNilPaymentsPage, testDate)
        .get
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .get
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .get

      val backendResponse = NilMonthlyReturnResponse(status = "STARTED")

      when(connector.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(backendResponse))
      when(sessionRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.createNilMonthlyReturn(userAnswers).futureValue

      val requestCaptor: ArgumentCaptor[NilMonthlyReturnRequest] =
        ArgumentCaptor.forClass(classOf[NilMonthlyReturnRequest])
      verify(connector).createNilMonthlyReturn(requestCaptor.capture())(any[HeaderCarrier])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.instanceId mustBe cisId
      capturedRequest.taxYear mustBe taxYear
      capturedRequest.taxMonth mustBe taxMonth
      capturedRequest.decNilReturnNoPayments mustBe "Y"
      capturedRequest.decInformationCorrect mustBe "Y"

      val sessionCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepo).set(sessionCaptor.capture())

      val updatedUserAnswers = sessionCaptor.getValue
      updatedUserAnswers.get(NilReturnStatusPage) mustBe Some("STARTED")
      result.get(NilReturnStatusPage) mustBe Some("STARTED")
    }

    "fail when CIS ID is missing from session" in {
      val (service, connector, sessionRepo) = newService()

      val userAnswers = UserAnswers("test-user")
        .set(DateConfirmNilPaymentsPage, java.time.LocalDate.of(2024, 10, 15))
        .get

      val ex = intercept[RuntimeException] {
        service.createNilMonthlyReturn(userAnswers).futureValue
      }
      ex.getMessage must include("CIS ID not found in session data")

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepo)
    }

    "fail when date is missing from session" in {
      val (service, connector, sessionRepo) = newService()

      val userAnswers = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .get

      val ex = intercept[RuntimeException] {
        service.createNilMonthlyReturn(userAnswers).futureValue
      }
      ex.getMessage must include("Date confirm nil payments not found in session data")

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepo)
    }

    "propagate failures from connector" in {
      val (service, connector, sessionRepo) = newService()

      val userAnswers = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .get
        .set(DateConfirmNilPaymentsPage, java.time.LocalDate.of(2024, 10, 15))
        .get
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .get
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .get

      when(connector.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("Backend error")))

      val ex = intercept[RuntimeException] {
        service.createNilMonthlyReturn(userAnswers).futureValue
      }
      ex.getMessage must include("Backend error")

      verify(connector).createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier])
      verifyNoInteractions(sessionRepo)
    }

    "propagate failures from session repository" in {
      val (service, connector, sessionRepo) = newService()

      val userAnswers = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .get
        .set(DateConfirmNilPaymentsPage, java.time.LocalDate.of(2024, 10, 15))
        .get
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .get
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .get

      when(connector.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(NilMonthlyReturnResponse(status = "STARTED")))
      when(sessionRepo.set(any[UserAnswers]))
        .thenReturn(Future.failed(new RuntimeException("Session error")))

      val ex = intercept[RuntimeException] {
        service.createNilMonthlyReturn(userAnswers).futureValue
      }
      ex.getMessage must include("Session error")

      verify(connector).createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier])
      verify(sessionRepo).set(any[UserAnswers])
    }

    "use placeholder N when declaration and inactivity are missing (C1 flow)" in {
      val (service, connector, sessionRepo) = newService()

      val cisId    = "CIS-123"
      val taxYear  = 2024
      val taxMonth = 10
      val testDate = java.time.LocalDate.of(taxYear, taxMonth, 15)

      val userAnswers = UserAnswers("test-user")
        .set(CisIdPage, cisId)
        .get
        .set(DateConfirmNilPaymentsPage, testDate)
        .get

      val backendResponse = NilMonthlyReturnResponse(status = "STARTED")

      when(connector.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(backendResponse))
      when(sessionRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.createNilMonthlyReturn(userAnswers).futureValue

      val requestCaptor: ArgumentCaptor[NilMonthlyReturnRequest] =
        ArgumentCaptor.forClass(classOf[NilMonthlyReturnRequest])
      verify(connector).createNilMonthlyReturn(requestCaptor.capture())(any[HeaderCarrier])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.instanceId mustBe cisId
      capturedRequest.taxYear mustBe taxYear
      capturedRequest.taxMonth mustBe taxMonth
      capturedRequest.decInformationCorrect mustBe "N"
      capturedRequest.decNilReturnNoPayments mustBe "N"

      val sessionCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepo).set(sessionCaptor.capture())

      val updatedUserAnswers = sessionCaptor.getValue
      updatedUserAnswers.get(NilReturnStatusPage) mustBe Some("STARTED")
      result.get(NilReturnStatusPage) mustBe Some("STARTED")
    }

    "use N when declaration is not confirmed" in {
      val (service, connector, sessionRepo) = newService()

      val userAnswers = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .get
        .set(DateConfirmNilPaymentsPage, java.time.LocalDate.of(2024, 10, 15))
        .get
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .get
        .set(DeclarationPage, Set.empty[Declaration])
        .get

      when(connector.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(NilMonthlyReturnResponse(status = "STARTED")))
      when(sessionRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      service.createNilMonthlyReturn(userAnswers).futureValue

      val requestCaptor: ArgumentCaptor[NilMonthlyReturnRequest] =
        ArgumentCaptor.forClass(classOf[NilMonthlyReturnRequest])
      verify(connector).createNilMonthlyReturn(requestCaptor.capture())(any[HeaderCarrier])
      requestCaptor.getValue.decInformationCorrect mustBe "N"
    }
  }

  "hasClient" should {

    "delegate to connector and return the boolean" in {
      val (service, connector, sessionRepo) = newService()

      when(connector.hasClient(eqTo("163"), eqTo("AB0063"))(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      service.hasClient("163", "AB0063").futureValue mustBe true

      verify(connector).hasClient(eqTo("163"), eqTo("AB0063"))(any[HeaderCarrier])
      verifyNoInteractions(sessionRepo)
    }
  }

  "createMonthlyReturn" should {

    "delegate to connector and complete successfully" in {
      val (service, connector, sessionRepo) = newService()

      val req = MonthlyReturnRequest(instanceId = "CIS-123", taxYear = 2024, taxMonth = 10)

      when(connector.createMonthlyReturn(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.createMonthlyReturn(req).futureValue mustBe ()

      verify(connector).createMonthlyReturn(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(sessionRepo)
    }
  }
}
