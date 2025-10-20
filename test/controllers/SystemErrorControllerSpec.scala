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
