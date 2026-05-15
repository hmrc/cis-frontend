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

package services.submission

import models.submission.GovTalkErrorStatus.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class GovTalkErrorClassifierSpec extends AnyWordSpec with Matchers {

  private def errorJs(number: String, errType: String, text: String) =
    Json.obj("number" -> number, "type" -> errType, "text" -> text)

  "GovTalkErrorClassifier" should {

    "classify DEPARTMENTAL_ERROR as DepartmentalError carrying the ChRIS text" in {
      val err = errorJs("3001", "business", "departmental boom")

      GovTalkErrorClassifier.classify("DEPARTMENTAL_ERROR", Some(err)) mustBe DepartmentalError("departmental boom")
    }

    "classify DEPARTMENTAL_ERROR without an error payload as DepartmentalError with empty text" in {
      GovTalkErrorClassifier.classify("DEPARTMENTAL_ERROR", None) mustBe DepartmentalError("")
    }

    List("3000", "2005", "1000").foreach { code =>
      s"classify STARTED + recoverable code $code as RecoverableError" in {
        val err = errorJs(code, "fatal", s"recoverable $code")

        GovTalkErrorClassifier.classify("STARTED", Some(err)) mustBe RecoverableError(code, s"recoverable $code")
      }

      s"classify FATAL_ERROR + recoverable code $code as RecoverableError" in {
        val err = errorJs(code, "fatal", s"recoverable $code")

        GovTalkErrorClassifier.classify("FATAL_ERROR", Some(err)) mustBe RecoverableError(code, s"recoverable $code")
      }
    }

    "classify FATAL_ERROR with a non-recoverable code as FatalError" in {
      val err = errorJs("9999", "fatal", "fatal boom")

      GovTalkErrorClassifier.classify("FATAL_ERROR", Some(err)) mustBe FatalError("9999", "fatal boom")
    }

    "classify FATAL_ERROR with no error payload as OtherStatus" in {
      GovTalkErrorClassifier.classify("FATAL_ERROR", None) mustBe OtherStatus
    }

    "classify any other status as OtherStatus" in {
      GovTalkErrorClassifier.classify("ACCEPTED", None) mustBe OtherStatus
      GovTalkErrorClassifier.classify("SUBMITTED", None) mustBe OtherStatus
      GovTalkErrorClassifier.classify("PENDING", None) mustBe OtherStatus
      GovTalkErrorClassifier.classify("TIMED_OUT", None) mustBe OtherStatus
    }
  }
}
