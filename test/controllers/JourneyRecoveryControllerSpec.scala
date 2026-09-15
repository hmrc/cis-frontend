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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}

import scala.concurrent.Future

class JourneyRecoveryControllerSpec extends SpecBase {

  "JourneyRecovery Controller" - {

    Seq(
      ("AGENT", true, applicationConfig.constructionIndustryAgentAccountUrl + "1"),
      ("ORGANISATION", false, applicationConfig.constructionIndustryOrgAccountUrl)
    ).foreach { case (accountTypeSTR, isAgent, cisAccountUrl) =>
      s"when accountType is '$accountTypeSTR'" - {

        "when a relative continue Url is supplied" - {

          "must return OK and the continue view" in {

            val userAnswer  = userAnswersWithCisId.set(CisIdPage, "1").success.value
            val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent)
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository(Some(userAnswer))))
              .build()

            running(application) {
              val continueUrl = RedirectUrl("/foo")
              val request     = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

              val result = route(application, request).value

              val continueView = application.injector.instanceOf[JourneyRecoveryContinueView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual continueView(continueUrl.unsafeValue)(
                request,
                applicationConfig,
                messages(application)
              ).toString
            }
          }
        }

        "when an absolute continue Url is supplied" - {

          "must return OK and the start again view" in {

            val userAnswer  = userAnswersWithCisId.set(CisIdPage, "1").success.value
            val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent)
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository(Some(userAnswer))))
              .build()

            running(application) {
              val continueUrl = RedirectUrl("https://foo.com")
              val request     = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

              val result = route(application, request).value

              val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual startAgainView(cisAccountUrl)(
                request,
                applicationConfig,
                messages(application)
              ).toString
            }
          }
        }

        "when no continue Url is supplied" - {

          "must return OK and the start again view" in {

            val userAnswer  = userAnswersWithCisId.set(CisIdPage, "1").success.value
            val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent)
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository(Some(userAnswer))))
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

              val result = route(application, request).value

              val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual startAgainView(cisAccountUrl)(
                request,
                applicationConfig,
                messages(application)
              ).toString
            }
          }
        }

        "when accountType is AGENT and CisIdPage is missing" - {

          "must return the agent account URL without a cisId appended" in {

            val application = applicationBuilder(userAnswers = None, isAgent = true)
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository(userAnswers = None)))
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

              val result = route(application, request).value

              val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

              status(result) mustEqual OK
              contentAsString(result) mustEqual startAgainView(applicationConfig.constructionIndustryAgentAccountUrl)(
                request,
                applicationConfig,
                messages(application)
              ).toString
            }
          }
        }

        "when the session store is unavailable" - {

          "must still render the start again view using the base account URL" in {

            val failingRepository = mock[SessionRepository]
            when(failingRepository.get(any[String])).thenReturn(Future.failed(new RuntimeException("Mongo down")))

            val application = applicationBuilder(isAgent = isAgent)
              .overrides(bind[SessionRepository].toInstance(failingRepository))
              .build()

            running(application) {
              val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

              val result = route(application, request).value

              val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

              val expectedUrl =
                if (!isAgent) {
                  applicationConfig.constructionIndustryOrgAccountUrl
                } else {
                  applicationConfig.constructionIndustryAgentAccountUrl
                }

              status(result) mustEqual OK
              contentAsString(result) mustEqual startAgainView(expectedUrl)(
                request,
                applicationConfig,
                messages(application)
              ).toString
            }
          }
        }
      }
    }
  }
}
