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

package controllers

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import org.mockito.Mockito.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import utils.{ReferenceGenerator, ReferenceGeneratorImpl}
import views.html.SystemErrorView

class SystemErrorControllerSpec extends SpecBase with Matchers {

  "SystemError Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockReferenceGenerator = mock(classOf[ReferenceGeneratorImpl])
      val expectedReference      = "YVN4HLUEHAUXVOB8"

      when(mockReferenceGenerator.generateReference()).thenReturn(expectedReference)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReferenceGenerator].toInstance(mockReferenceGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SystemErrorController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SystemErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedReference)(
          request,
          applicationConfig,
          messages(application)
        ).toString
      }
    }
  }
}
