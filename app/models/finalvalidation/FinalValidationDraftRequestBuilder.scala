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

import models.monthlyreturns.Subcontractor

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class FinalValidationDraftRequestBuilder @Inject() () {

  def build(
    instanceId: String,
    selectedSubcontractors: Seq[Subcontractor],
    validation: FinalValidationResult
  ): Try[CreateFinalValidationDraftRequest] = {

    val selectedById =
      selectedSubcontractors.map(subcontractor => subcontractor.subcontractorId -> subcontractor).toMap

    validation.failures
      .foldLeft(
        Try(Seq.empty[CreateFinalValidationDraftSubcontractor])
      ) { case (acc, failure) =>
        for {
          existing <- acc

          subcontractor <-
            selectedById
              .get(failure.subcontractorId)
              .map(Success(_))
              .getOrElse(
                Failure(
                  new IllegalStateException(
                    s"Subcontractor ${failure.subcontractorId} not found in selected subcontractors"
                  )
                )
              )

          subbieResourceRef <-
            subcontractor.subbieResourceRef
              .map(Success(_))
              .getOrElse(
                Failure(
                  new IllegalStateException(s"Missing subbieResourceRef for ${failure.subcontractorId}")
                )
              )
        } yield existing :+
          CreateFinalValidationDraftSubcontractor(
            subcontractorId = subcontractor.subcontractorId,
            subbieResourceRef = subbieResourceRef,
            baseVersion = subcontractor.version,
            subcontractorType = subcontractor.subcontractorType,
            displayName = subcontractor.displayName.getOrElse(""),
            details = toDetails(subcontractor),
            issues = failure.issues.map { issue =>
              FinalValidationDraftIssue(issue.field.key, issue.value)
            }
          )
      }
      .map { subcontractors =>
        CreateFinalValidationDraftRequest(
          instanceId = instanceId,
          context = "MonthlyReturn",
          subcontractors = subcontractors
        )
      }
  }

  private def toDetails(
    subcontractor: Subcontractor
  ): FinalValidationSubcontractorDetails =
    FinalValidationSubcontractorDetails(
      firstName = subcontractor.firstName,
      secondName = subcontractor.secondName,
      surname = subcontractor.surname,
      partnershipTradingName = subcontractor.partnershipTradingName,
      tradingName = subcontractor.tradingName,
      addressLine1 = subcontractor.addressLine1,
      addressLine2 = subcontractor.addressLine2,
      addressLine3 = subcontractor.addressLine3,
      addressLine4 = subcontractor.addressLine4,
      country = subcontractor.country,
      postcode = subcontractor.postCode,
      emailAddress = subcontractor.emailAddress,
      phoneNumber = subcontractor.phoneNumber,
      mobilePhoneNumber = subcontractor.mobilePhoneNumber,
      utr = subcontractor.utr,
      partnerUtr = subcontractor.partnerUtr,
      nino = subcontractor.nino,
      crn = subcontractor.crn,
      worksReferenceNumber = subcontractor.worksReferenceNumber
    )
}
