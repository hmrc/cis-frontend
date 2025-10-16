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

import config.FrontendAppConfig
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

import java.time.LocalDateTime

class PollingServiceSpec extends AnyWordSpec with Matchers {

  val testConfig             = Configuration(
    "submission-poll-timeout-seconds" -> 60
  )
  private val submissionYear = 2025
  private val submissionDate = LocalDateTime.of(submissionYear, 1, 1, 0, 0, 0)

  private def service = new PollingService(new FrontendAppConfig(testConfig))

  "calculatePollTimeout" should {

    "return the correct timeout time" in {

      val result = service.calculatePollTimeout(submissionDate)

      val expected = submissionDate.plusMinutes(1)
      result mustBe expected
    }

  }

}
