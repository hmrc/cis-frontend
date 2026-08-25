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

package controllers.finalvalidations

import base.SpecBase
import models.UserAnswers
import models.monthlyreturns.{GetAllMonthlyReturnDetailsResponse, Subcontractor}
import models.requests.GetMonthlyReturnForEditRequest
import models.validation.SubcontractorValidationField.EmailAddress
import models.validation.{FieldValidationFailure, SubcontractorValidationFailure}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import pages.validation.SubcontractorValidationFailuresPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.finalvalidations.ReviewSubcontractorDetailsView

import java.time.LocalDate
import scala.concurrent.Future

class ReviewSubcontractorDetailsControllerSpec extends SpecBase {

  private val request =
    FakeRequest(
      GET,
      routes.ReviewSubcontractorDetailsController.onPageLoad().url
    )

  private val failures =
    List(
      validationFailure(2L),
      validationFailure(1L),
      validationFailure(99L)
    )

  private val userAnswers =
    emptyUserAnswers
      .setOrException(CisIdPage, "CIS-123")
      .setOrException(
        DateConfirmPaymentsPage,
        LocalDate.of(2026, 8, 5)
      )
      .setOrException(
        SubcontractorValidationFailuresPage,
        failures
      )

  private val response =
    GetAllMonthlyReturnDetailsResponse(
      scheme = Seq.empty,
      monthlyReturn = Seq.empty,
      subcontractors = Seq(
        subcontractor(1L, Some("First Subcontractor")),
        subcontractor(2L, Some("Second Subcontractor"))
      ),
      monthlyReturnItems = Seq.empty,
      submission = Seq.empty
    )

  "ReviewSubcontractorDetailsController.onPageLoad" - {

    "read stored failures and render subcontractor names in failure order" in {
      val monthlyReturnService =
        mock[MonthlyReturnService]

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(
          any[GetMonthlyReturnForEditRequest]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(response))

      val application =
        applicationWith(
          userAnswers = userAnswers,
          monthlyReturnService = monthlyReturnService
        )

      running(application) {
        val result = route(application, request).value

        val view =
          application.injector
            .instanceOf[ReviewSubcontractorDetailsView]

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(
            Seq(
              "Second Subcontractor",
              "First Subcontractor",
              "No name provided"
            )
          )(
            request,
            messages(application)
          ).toString

        verify(monthlyReturnService)
          .retrieveMonthlyReturnForEditDetails(
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
      }
    }

    "redirect to JourneyRecovery when stored failures are missing" in {
      val monthlyReturnService =
        mock[MonthlyReturnService]

      val answersWithoutFailures =
        userAnswers
          .remove(SubcontractorValidationFailuresPage)
          .get

      val application =
        applicationWith(
          userAnswers = answersWithoutFailures,
          monthlyReturnService = monthlyReturnService
        )

      running(application) {
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url

        verify(monthlyReturnService, never)
          .retrieveMonthlyReturnForEditDetails(
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
      }
    }

    "redirect to JourneyRecovery when stored failures are empty" in {
      val monthlyReturnService =
        mock[MonthlyReturnService]

      val answersWithEmptyFailures =
        userAnswers
          .setOrException(
            SubcontractorValidationFailuresPage,
            Nil
          )

      val application =
        applicationWith(
          userAnswers = answersWithEmptyFailures,
          monthlyReturnService = monthlyReturnService
        )

      running(application) {
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url

        verify(monthlyReturnService, never)
          .retrieveMonthlyReturnForEditDetails(
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
      }
    }

    "redirect to JourneyRecovery when the monthly-return request cannot be built" in {
      val monthlyReturnService =
        mock[MonthlyReturnService]

      val answersWithoutDate =
        userAnswers
          .remove(DateConfirmPaymentsPage)
          .get

      val application =
        applicationWith(
          userAnswers = answersWithoutDate,
          monthlyReturnService = monthlyReturnService
        )

      running(application) {
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url

        verify(monthlyReturnService, never)
          .retrieveMonthlyReturnForEditDetails(
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
      }
    }

    "redirect to SystemError when subcontractor details cannot be retrieved" in {
      val monthlyReturnService =
        mock[MonthlyReturnService]

      when(
        monthlyReturnService.retrieveMonthlyReturnForEditDetails(
          any[GetMonthlyReturnForEditRequest]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.failed(
          new RuntimeException("retrieve failed")
        )
      )

      val application =
        applicationWith(
          userAnswers = userAnswers,
          monthlyReturnService = monthlyReturnService
        )

      running(application) {
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.SystemErrorController
            .onPageLoad()
            .url
      }
    }
  }

  private def applicationWith(
    userAnswers: UserAnswers,
    monthlyReturnService: MonthlyReturnService
  ) =
    applicationBuilder(
      userAnswers = Some(userAnswers),
      additionalBindings = Seq(
        bind[MonthlyReturnService]
          .toInstance(monthlyReturnService)
      )
    ).build()

  private def validationFailure(
    subcontractorId: Long
  ): SubcontractorValidationFailure =
    SubcontractorValidationFailure(
      subcontractorId = subcontractorId,
      failedFields = List(
        FieldValidationFailure(
          field = EmailAddress,
          value = Some("invalid-email")
        )
      )
    )

  private def subcontractor(
    subcontractorId: Long,
    displayName: Option[String]
  ): Subcontractor =
    Subcontractor(
      subcontractorId = subcontractorId,
      utr = Some("1234567890"),
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = Some("John"),
      nino = Some("AA123456A"),
      secondName = None,
      surname = Some("Smith"),
      partnershipTradingName = None,
      tradingName = None,
      subcontractorType = Some("soletrader"),
      addressLine1 = Some("1 High Street"),
      addressLine2 = Some("Newcastle"),
      addressLine3 = None,
      addressLine4 = None,
      country = Some("United Kingdom"),
      postCode = Some("NE1 1AA"),
      emailAddress = Some("subcontractor@example.com"),
      phoneNumber = Some("0191 123 4567"),
      mobilePhoneNumber = Some("07700 900123"),
      worksReferenceNumber = None,
      createDate = None,
      lastUpdate = None,
      subbieResourceRef = Some(subcontractorId * 10),
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
      displayName = displayName
    )
}
