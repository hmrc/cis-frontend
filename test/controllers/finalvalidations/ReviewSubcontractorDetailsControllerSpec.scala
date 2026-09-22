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
import models.finalvalidation.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.monthlyreturns.CisIdPage
import pages.finalvalidations.{FinalValidationDraftIdPage, FinalValidationVerificationRequiredPage, MonthlyFinalValidationSourcePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.libs.json.Json
import repositories.SessionRepository
import services.finalvalidation.FinalValidationDraftService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.finalvalidations.ReviewSubcontractorDetailsView
import models.NormalMode

import scala.concurrent.Future

class ReviewSubcontractorDetailsControllerSpec extends SpecBase {

  private val draftId = "draft-123"

  private val request =
    FakeRequest(
      GET,
      routes.ReviewSubcontractorDetailsController.onPageLoad().url
    )

  private val submitRequest =
    FakeRequest(
      POST,
      routes.ReviewSubcontractorDetailsController.onSubmit().url
    )

  private val userAnswers =
    emptyUserAnswers
      .setOrException(CisIdPage, "CIS-123")
      .setOrException(
        FinalValidationDraftIdPage,
        draftId
      )
      .setOrException(
        MonthlyFinalValidationSourcePage,
        MonthlyFinalValidationSource.SelectSubcontractors
      )

  private def draft(
    firstReadiness: String,
    secondReadiness: String
  ): FinalValidationDraft =
    Json
      .obj(
        "subcontractors" -> Json.arr(
          Json.obj(
            "subcontractorId"   -> 1L,
            "subbieResourceRef" -> 10L,
            "baseVersion"       -> 1,
            "subcontractorType" -> "soletrader",
            "displayName"       -> "First Subcontractor",
            "base"              -> Json.obj(
              "firstName" -> "First",
              "surname"   -> "Subcontractor"
            ),
            "proposed"          -> Json.obj(
              "firstName" -> "First",
              "surname"   -> "Subcontractor"
            ),
            "changedTargets"    -> Json.arr(),
            "issues"            -> Json.arr(),
            "readiness"         -> firstReadiness,
            "commitStatus"      -> "Pending"
          ),
          Json.obj(
            "subcontractorId"   -> 2L,
            "subbieResourceRef" -> 20L,
            "baseVersion"       -> 1,
            "subcontractorType" -> "soletrader",
            "displayName"       -> "Second Subcontractor",
            "base"              -> Json.obj(
              "firstName" -> "Second",
              "surname"   -> "Subcontractor"
            ),
            "proposed"          -> Json.obj(
              "firstName" -> "Second",
              "surname"   -> "Subcontractor"
            ),
            "changedTargets"    -> Json.arr(),
            "issues"            -> Json.arr(),
            "readiness"         -> secondReadiness,
            "commitStatus"      -> "Pending"
          )
        )
      )
      .as[FinalValidationDraft]

  "ReviewSubcontractorDetailsController.onPageLoad" - {

    "render subcontractors from the Final Validation draft" in {
      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val sessionRepository =
        mock[SessionRepository]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(
            firstReadiness = "Incomplete",
            secondReadiness = "Complete"
          )
        )
      )

      val application =
        applicationWith(
          userAnswers = userAnswers,
          finalValidationDraftService = finalValidationDraftService,
          sessionRepository = sessionRepository
        )

      running(application) {
        val result =
          route(application, request).value

        val view =
          application.injector
            .instanceOf[ReviewSubcontractorDetailsView]

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(
            ReviewSubcontractorDetailsPageModel(
              Seq(
                ReviewSubcontractorDetailsRow(
                  1L,
                  "First Subcontractor",
                  true
                ),
                ReviewSubcontractorDetailsRow(
                  2L,
                  "Second Subcontractor",
                  false
                )
              ),
              false,
              controllers.monthlyreturns.routes.SelectSubcontractorsController
                .onPageLoad(None)
                .url
            )
          )(
            request,
            messages(application)
          ).toString

        verify(finalValidationDraftService)
          .get(
            any[String],
            any[String]
          )(any[HeaderCarrier])
      }
    }

    "redirect to JourneyRecovery when there is no Final Validation draft id" in {
      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val sessionRepository =
        mock[SessionRepository]

      val answersWithoutDraftId =
        userAnswers
          .remove(FinalValidationDraftIdPage)
          .get

      val application =
        applicationWith(
          userAnswers = answersWithoutDraftId,
          finalValidationDraftService = finalValidationDraftService,
          sessionRepository = sessionRepository
        )

      running(application) {
        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }
  }

  "ReviewSubcontractorDetailsController.onSubmit" - {

    "redirect back to review when the draft is incomplete" in {
      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val sessionRepository =
        mock[SessionRepository]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(
            firstReadiness = "Complete",
            secondReadiness = "Incomplete"
          )
        )
      )

      val application =
        applicationWith(
          userAnswers = userAnswers,
          finalValidationDraftService = finalValidationDraftService,
          sessionRepository = sessionRepository
        )

      running(application) {
        val result =
          route(application, submitRequest).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          routes.ReviewSubcontractorDetailsController
            .onPageLoad()
            .url
      }
    }

    "commit and continue to VerifySubcontractors when verification is required" in {
      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val sessionRepository =
        mock[SessionRepository]

      val answers =
        userAnswers.setOrException(
          FinalValidationVerificationRequiredPage,
          true
        )

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(
            firstReadiness = "Complete",
            secondReadiness = "Complete"
          )
        )
      )

      when(
        finalValidationDraftService.commit(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        sessionRepository.set(any())
      ).thenReturn(Future.successful(true))

      val application =
        applicationWith(
          userAnswers = answers,
          finalValidationDraftService = finalValidationDraftService,
          sessionRepository = sessionRepository
        )

      running(application) {
        val result =
          route(application, submitRequest).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.VerifySubcontractorsController
            .onPageLoad(NormalMode)
            .url

        verify(finalValidationDraftService)
          .commit(
            any[String],
            any[String]
          )(any[HeaderCarrier])

        val userAnswersCaptor =
          ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(sessionRepository)
          .set(userAnswersCaptor.capture())

        userAnswersCaptor.getValue
          .get(FinalValidationDraftIdPage) mustBe None

        userAnswersCaptor.getValue
          .get(MonthlyFinalValidationSourcePage) mustBe None

        userAnswersCaptor.getValue
          .get(FinalValidationVerificationRequiredPage) mustBe None
      }
    }

    "commit and continue to SubcontractorDetailsAdded when verification is not required" in {
      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val sessionRepository =
        mock[SessionRepository]

      val answers =
        userAnswers.setOrException(
          FinalValidationVerificationRequiredPage,
          false
        )

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(
            firstReadiness = "Complete",
            secondReadiness = "Complete"
          )
        )
      )

      when(
        finalValidationDraftService.commit(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        sessionRepository.set(any())
      ).thenReturn(Future.successful(true))

      val application =
        applicationWith(
          userAnswers = answers,
          finalValidationDraftService = finalValidationDraftService,
          sessionRepository = sessionRepository
        )

      running(application) {
        val result =
          route(application, submitRequest).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoad(NormalMode)
            .url

        verify(finalValidationDraftService)
          .commit(
            any[String],
            any[String]
          )(any[HeaderCarrier])

        verify(sessionRepository)
          .set(any())
      }
    }

    "redirect to JourneyRecovery when the source is missing" in {
      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val sessionRepository =
        mock[SessionRepository]

      val answersWithoutSource =
        userAnswers
          .remove(MonthlyFinalValidationSourcePage)
          .get

      val application =
        applicationWith(
          userAnswers = answersWithoutSource,
          finalValidationDraftService = finalValidationDraftService,
          sessionRepository = sessionRepository
        )

      running(application) {
        val result =
          route(application, submitRequest).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }
  }

  private def applicationWith(
    userAnswers: UserAnswers,
    finalValidationDraftService: FinalValidationDraftService,
    sessionRepository: SessionRepository
  ) =
    applicationBuilder(
      userAnswers = Some(userAnswers),
      additionalBindings = Seq(
        bind[FinalValidationDraftService]
          .toInstance(finalValidationDraftService),
        bind[SessionRepository]
          .toInstance(sessionRepository)
      )
    ).build()
}
