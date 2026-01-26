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

package models.monthlyreturns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

final class GetAllMonthlyReturnDetailsResponseSpec extends AnyFreeSpec with Matchers {

  "GetAllMonthlyReturnDetailsResponse JSON" - {

    "writes and reads round-trip with populated collections" in {
      val scheme = ContractorScheme(
        schemeId = 1,
        instanceId = "CIS-123",
        accountsOfficeReference = "123PA12345678",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456"
      )

      val monthlyReturn = MonthlyReturn(
        monthlyReturnId = 101L,
        taxYear = 2025,
        taxMonth = 10
      )

      val subcontractor = Subcontractor(
        subcontractorId = 1001L,
        utr = Some("1234567890"),
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
        pendingVerifications = None
      )

      val monthlyReturnItem = MonthlyReturnItem(
        monthlyReturnId = 101L,
        monthlyReturnItemId = 2001L,
        totalPayments = Some("1000.00"),
        costOfMaterials = Some("200.00"),
        totalDeducted = Some("150.00"),
        unmatchedTaxRateIndicator = None,
        subcontractorId = Some(1001L),
        subcontractorName = Some("Test Subcontractor Ltd"),
        verificationNumber = Some("V123456"),
        itemResourceReference = None
      )

      val submission = Submission(
        submissionId = 3001L,
        submissionType = "MONTHLY_RETURN",
        activeObjectId = Some(101L),
        status = Some("SUBMITTED"),
        hmrcMarkGenerated = Some("ABC123"),
        hmrcMarkGgis = None,
        emailRecipient = Some("test@example.com"),
        acceptedTime = None,
        createDate = None,
        lastUpdate = None,
        schemeId = 1L,
        agentId = None,
        l_Migrated = None,
        submissionRequestDate = None,
        govTalkErrorCode = None,
        govTalkErrorType = None,
        govTalkErrorMessage = None
      )

      val response = GetAllMonthlyReturnDetailsResponse(
        scheme = Seq(scheme),
        monthlyReturn = Seq(monthlyReturn),
        subcontractors = Seq(subcontractor),
        monthlyReturnItems = Seq(monthlyReturnItem),
        submission = Seq(submission)
      )

      val js = Json.toJson(response)

      (js \ "scheme").as[Seq[ContractorScheme]] mustBe Seq(scheme)
      (js \ "monthlyReturn").as[Seq[MonthlyReturn]] mustBe Seq(monthlyReturn)
      (js \ "subcontractors").as[Seq[Subcontractor]] mustBe Seq(subcontractor)
      (js \ "monthlyReturnItems").as[Seq[MonthlyReturnItem]] mustBe Seq(monthlyReturnItem)
      (js \ "submission").as[Seq[Submission]] mustBe Seq(submission)

      Json.fromJson[GetAllMonthlyReturnDetailsResponse](js) mustBe JsSuccess(response)
    }

    "writes and reads round-trip with empty collections" in {
      val response = GetAllMonthlyReturnDetailsResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val js = Json.toJson(response)

      (js \ "scheme").as[Seq[ContractorScheme]] mustBe empty
      (js \ "monthlyReturn").as[Seq[MonthlyReturn]] mustBe empty
      (js \ "subcontractors").as[Seq[Subcontractor]] mustBe empty
      (js \ "monthlyReturnItems").as[Seq[MonthlyReturnItem]] mustBe empty
      (js \ "submission").as[Seq[Submission]] mustBe empty

      Json.fromJson[GetAllMonthlyReturnDetailsResponse](js) mustBe JsSuccess(response)
    }

    "reads from JSON with minimal nested objects" in {
      val jsonString =
        """{
          |  "scheme": [{
          |    "schemeId": 1,
          |    "instanceId": "CIS-456",
          |    "accountsOfficeReference": "456PA87654321",
          |    "taxOfficeNumber": "456",
          |    "taxOfficeReference": "CD789"
          |  }],
          |  "monthlyReturn": [{
          |    "monthlyReturnId": 200,
          |    "taxYear": 2024,
          |    "taxMonth": 5
          |  }],
          |  "subcontractors": [{
          |    "subcontractorId": 2001
          |  }],
          |  "monthlyReturnItems": [{
          |    "monthlyReturnId": 200,
          |    "monthlyReturnItemId": 3001
          |  }],
          |  "submission": [{
          |    "submissionId": 4001,
          |    "submissionType": "NIL_RETURN",
          |    "schemeId": 1
          |  }]
          |}""".stripMargin

      val js     = Json.parse(jsonString)
      val result = js.as[GetAllMonthlyReturnDetailsResponse]

      result.scheme must have length 1
      result.scheme.head.schemeId mustBe 1
      result.scheme.head.instanceId mustBe "CIS-456"

      result.monthlyReturn must have length 1
      result.monthlyReturn.head.monthlyReturnId mustBe 200L
      result.monthlyReturn.head.taxYear mustBe 2024
      result.monthlyReturn.head.taxMonth mustBe 5

      result.subcontractors must have length 1
      result.subcontractors.head.subcontractorId mustBe 2001L
      result.subcontractors.head.tradingName mustBe None

      result.monthlyReturnItems must have length 1
      result.monthlyReturnItems.head.monthlyReturnId mustBe 200L
      result.monthlyReturnItems.head.totalPayments mustBe None

      result.submission must have length 1
      result.submission.head.submissionId mustBe 4001L
      result.submission.head.submissionType mustBe "NIL_RETURN"
    }

    "reads from JSON with multiple items in each collection" in {
      val jsonString =
        """{
          |  "scheme": [
          |    {"schemeId": 1, "instanceId": "CIS-1", "accountsOfficeReference": "AO1", "taxOfficeNumber": "100", "taxOfficeReference": "REF1"},
          |    {"schemeId": 2, "instanceId": "CIS-2", "accountsOfficeReference": "AO2", "taxOfficeNumber": "200", "taxOfficeReference": "REF2"}
          |  ],
          |  "monthlyReturn": [
          |    {"monthlyReturnId": 101, "taxYear": 2025, "taxMonth": 1},
          |    {"monthlyReturnId": 102, "taxYear": 2025, "taxMonth": 2}
          |  ],
          |  "subcontractors": [
          |    {"subcontractorId": 1001, "tradingName": "Sub A"},
          |    {"subcontractorId": 1002, "tradingName": "Sub B"},
          |    {"subcontractorId": 1003, "tradingName": "Sub C"}
          |  ],
          |  "monthlyReturnItems": [
          |    {"monthlyReturnId": 101, "monthlyReturnItemId": 2001},
          |    {"monthlyReturnId": 102, "monthlyReturnItemId": 2002}
          |  ],
          |  "submission": [
          |    {"submissionId": 3001, "submissionType": "MONTHLY_RETURN", "schemeId": 1}
          |  ]
          |}""".stripMargin

      val js     = Json.parse(jsonString)
      val result = js.as[GetAllMonthlyReturnDetailsResponse]

      result.scheme             must have length 2
      result.monthlyReturn      must have length 2
      result.subcontractors     must have length 3
      result.monthlyReturnItems must have length 2
      result.submission         must have length 1

      result.subcontractors.map(_.tradingName) mustBe Seq(Some("Sub A"), Some("Sub B"), Some("Sub C"))
    }
  }
}
