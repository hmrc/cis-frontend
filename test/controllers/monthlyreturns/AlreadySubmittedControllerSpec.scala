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
import pages.monthlyreturns.{CisIdPage, ReturnTypePage}
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.AlreadySubmittedView

import java.util.UUID

class AlreadySubmittedControllerSpec extends SpecBase {

  "AlreadySubmitted Controller" - {

    Seq(
      ("AGENT", true, applicationConfig.constructionIndustryAgentAccountUrl + "1"),
      ("ORGANISATION", false, applicationConfig.constructionIndustryOrgAccountUrl)
    ).foreach { case (accountTypeSTR, isAgent, cisAccountUrl) =>
      s"when accountType is '$accountTypeSTR'" - {

        "must return OK and the correct view for a GET for nil return" in {
          val userAnswer  = userAnswersWithCisId.set(ReturnTypePage, MonthlyNilReturn).success.value
          val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[AlreadySubmittedView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              "monthlyreturns.alreadySubmitted.nilreturn",
              cisAccountUrl
            )(
              viewRequest(request, isAgent),
              messages(application)
            ).toString
          }
        }

        "must return OK and the correct view for a GET for standard return" in {
          val userAnswer  = userAnswersWithCisId.set(ReturnTypePage, MonthlyStandardReturn).success.value
          val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent).build()

          running(application) {
            val request =
              FakeRequest(GET, controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[AlreadySubmittedView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              "monthlyreturns.alreadySubmitted",
              cisAccountUrl
            )(
              viewRequest(request, isAgent),
              messages(application)
            ).toString
          }
        }

        "throw IllegalStateException when ReturnType is missing" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

          running(application) {

            val request =
              FakeRequest(GET, controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url)

            val controller = application.injector.instanceOf[AlreadySubmittedController]

            val exception = controller.onPageLoad()(request).failed.futureValue

            exception mustBe a[IllegalStateException]
            exception.getMessage mustBe "ReturnTypePage missing from userAnswers"
          }
        }

        // Multi-tab: UUID-scoped journey tests
        "must return OK and the correct view for a GET for ID-scoped nil return" in {
          val flowId = UUID.randomUUID()
          val id     = flowId.toString

          val userAnswer = userAnswersWithCisId
            .set(CisIdPage(id), "2")
            .success
            .value
            .set(ReturnTypePage, MonthlyStandardReturn)
            .success
            .value
            .set(ReturnTypePage(id), MonthlyNilReturn)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent)
            .configure("features.multi-tab-enabled" -> true)
            .build()

          running(application) {
            val request =
              FakeRequest(
                GET,
                controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoadWithId(flowId).url
              )

            val result = route(application, request).value

            val view = application.injector.instanceOf[AlreadySubmittedView]

            val expectedCisAccountUrl =
              if (isAgent) applicationConfig.constructionIndustryAgentAccountUrl + "2"
              else applicationConfig.constructionIndustryOrgAccountUrl

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              "monthlyreturns.alreadySubmitted.nilreturn",
              expectedCisAccountUrl
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must return OK and the correct view for a GET for ID-scoped standard return" in {
          val flowId = UUID.randomUUID()
          val id     = flowId.toString

          val userAnswer = userAnswersWithCisId
            .set(CisIdPage(id), "2")
            .success
            .value
            .set(ReturnTypePage, MonthlyNilReturn)
            .success
            .value
            .set(ReturnTypePage(id), MonthlyStandardReturn)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent)
            .configure("features.multi-tab-enabled" -> true)
            .build()

          running(application) {
            val request =
              FakeRequest(
                GET,
                controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoadWithId(flowId).url
              )

            val result = route(application, request).value

            val view = application.injector.instanceOf[AlreadySubmittedView]

            val expectedCisAccountUrl =
              if isAgent then applicationConfig.constructionIndustryAgentAccountUrl + "2"
              else applicationConfig.constructionIndustryOrgAccountUrl

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              "monthlyreturns.alreadySubmitted",
              expectedCisAccountUrl
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "throw IllegalStateException when ID-scoped ReturnType is missing" in {
          val flowId = UUID.randomUUID()
          val id     = flowId.toString

          val userAnswer = userAnswersWithCisId
            .set(CisIdPage(id), "2")
            .success
            .value
            .set(ReturnTypePage, MonthlyNilReturn)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswer), isAgent = isAgent)
            .configure("features.multi-tab-enabled" -> true)
            .build()

          running(application) {
            val request =
              FakeRequest(
                GET,
                controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoadWithId(flowId).url
              )

            val controller = application.injector.instanceOf[AlreadySubmittedController]

            val exception = controller.onPageLoadWithId(flowId)(request).failed.futureValue

            exception mustBe a[IllegalStateException]
            exception.getMessage mustBe "ReturnTypePage missing from userAnswers"
          }
        }
      }
    }
  }
}
