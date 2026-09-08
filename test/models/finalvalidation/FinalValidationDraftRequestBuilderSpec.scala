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

package models.finalvalidation

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar.*
import models.monthlyreturns.Subcontractor
import org.mockito.Mockito.when

class FinalValidationDraftRequestBuilderSpec extends SpecBase {

  private val builder = new FinalValidationDraftRequestBuilder()

  private def subcontractor(
    id: Long = 1L,
    resourceRef: Option[Long] = Some(100L)
  ): Subcontractor = {

    val subcontractor = mock[Subcontractor]

    when(subcontractor.subcontractorId).thenReturn(id)
    when(subcontractor.subbieResourceRef).thenReturn(resourceRef)
    when(subcontractor.version).thenReturn(Some(2))
    when(subcontractor.subcontractorType).thenReturn(Some("Individual"))
    when(subcontractor.displayName).thenReturn(Some("John Smith"))

    when(subcontractor.firstName).thenReturn(Some("John"))
    when(subcontractor.secondName).thenReturn(None)
    when(subcontractor.surname).thenReturn(Some("Smith"))
    when(subcontractor.partnershipTradingName).thenReturn(None)
    when(subcontractor.tradingName).thenReturn(None)
    when(subcontractor.addressLine1).thenReturn(None)
    when(subcontractor.addressLine2).thenReturn(None)
    when(subcontractor.addressLine3).thenReturn(None)
    when(subcontractor.addressLine4).thenReturn(None)
    when(subcontractor.country).thenReturn(None)
    when(subcontractor.postCode).thenReturn(None)
    when(subcontractor.emailAddress).thenReturn(None)
    when(subcontractor.phoneNumber).thenReturn(None)
    when(subcontractor.mobilePhoneNumber).thenReturn(None)
    when(subcontractor.utr).thenReturn(Some("1234567890"))
    when(subcontractor.partnerUtr).thenReturn(None)
    when(subcontractor.nino).thenReturn(None)
    when(subcontractor.crn).thenReturn(None)
    when(subcontractor.worksReferenceNumber).thenReturn(None)

    subcontractor
  }

  private val failure =
    SubcontractorFinalValidationFailure(
      subcontractorId = 1L,
      issues = Seq(
        FinalValidationIssue(
          field = FinalValidationField.Utr,
          value = Some("1234567890")
        )
      ),
      subbieResourceRef = Some(100L)
    )

  "FinalValidationDraftRequestBuilder.build" - {

    "build a request" in {
      val result = builder
        .build(
          instanceId = "instance-1",
          selectedSubcontractors = Seq(subcontractor()),
          validation = FinalValidationResult(Seq(failure))
        )
        .get

      result.instanceId mustBe "instance-1"
      result.context mustBe "MonthlyReturn"

      result.subcontractors.size mustBe 1

      val resultSubcontractor = result.subcontractors.head

      resultSubcontractor.subcontractorId mustBe 1L
      resultSubcontractor.subbieResourceRef mustBe 100L
      resultSubcontractor.displayName mustBe "John Smith"
      resultSubcontractor.details.firstName mustBe Some("John")

      resultSubcontractor.issues mustBe Seq(
        FinalValidationDraftIssue(
          fieldKey = "utr",
          value = Some("1234567890")
        )
      )
    }

    "fail when the subcontractor cannot be found" in {
      val result = builder.build(
        instanceId = "instance-1",
        selectedSubcontractors = Seq.empty,
        validation = FinalValidationResult(Seq(failure))
      )

      result.failed.get.getMessage mustBe
        "Subcontractor 1 not found in selected subcontractors"
    }

    "fail when subbieResourceRef is missing" in {
      val result = builder.build(
        instanceId = "instance-1",
        selectedSubcontractors = Seq(
          subcontractor(resourceRef = None)
        ),
        validation = FinalValidationResult(Seq(failure))
      )

      result.failed.get.getMessage mustBe
        "Missing subbieResourceRef for 1"
    }
  }
}
