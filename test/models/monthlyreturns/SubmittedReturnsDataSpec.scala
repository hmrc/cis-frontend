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

package models.monthlyreturns

import base.SpecBase
import play.api.libs.json.Json

import java.time.LocalDateTime

class SubmittedReturnsDataSpec extends SpecBase {

  private val submission = SubmittedSubmissionData(
    submissionId = 3001L,
    submissionType = Some("MONTHLY_RETURN"),
    activeObjectId = Some(101L),
    status = "SUBMITTED",
    hmrcMarkGenerated = Some("IRMARK123"),
    hmrcMarkGgis = Some("IRMARK123"),
    emailRecipient = Some("test@test.com"),
    acceptedTime = Some(LocalDateTime.parse("2025-10-05T12:30:45"))
  )

  private val submittedReturns = SubmittedReturnsData(
    scheme = SubmittedSchemeData(
      name = "Test Contractor",
      taxOfficeNumber = "123",
      taxOfficeReference = "AB456"
    ),
    monthlyReturns = Seq(
      SubmittedMonthlyReturnData(
        monthlyReturnId = 101L,
        taxYear = 2025,
        taxMonth = 10,
        nilReturnIndicator = "N",
        status = "SUBMITTED",
        amendment = "N",
        supersededBy = None,
        amendmentStatus = None,
        monthlyReturnItems = None
      )
    ),
    submissions = Seq(submission)
  )

  "SubmittedReturnsData" - {

    "must serialise and deserialise submitted returns" in {
      Json.toJson(submittedReturns).as[SubmittedReturnsData] mustBe submittedReturns
    }

    "must deserialise empty collections" in {
      val data = submittedReturns.copy(
        monthlyReturns = Seq.empty,
        submissions = Seq.empty
      )

      Json.toJson(data).as[SubmittedReturnsData] mustBe data
    }
  }

  "SubmittedSubmissionData" - {

    "must deserialise acceptedTime containing a timezone suffix" in {
      val json = Json.toJsObject(submission) ++ Json.obj(
        "acceptedTime" -> "2025-10-05T12:30:45Z"
      )

      json.as[SubmittedSubmissionData] mustBe submission
    }

    "must deserialise missing optional fields" in {
      val json = Json.obj(
        "submissionId" -> 3001L,
        "status"       -> "SUBMITTED_NO_RECEIPT"
      )

      json.as[SubmittedSubmissionData] mustBe SubmittedSubmissionData(
        submissionId = 3001L,
        submissionType = None,
        activeObjectId = None,
        status = "SUBMITTED_NO_RECEIPT",
        hmrcMarkGenerated = None,
        hmrcMarkGgis = None,
        emailRecipient = None,
        acceptedTime = None
      )
    }

    "must fail to deserialise an invalid acceptedTime" in {
      val json = Json.toJsObject(submission) ++ Json.obj(
        "acceptedTime" -> "invalid-date"
      )

      json.validate[SubmittedSubmissionData].isError mustBe true
    }

    "must fail to deserialise a non-string acceptedTime" in {
      val json = Json.toJsObject(submission) ++ Json.obj(
        "acceptedTime" -> 123
      )

      json.validate[SubmittedSubmissionData].isError mustBe true
    }
  }
}
