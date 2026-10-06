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

import play.api.libs.json.*

case class FinalValidationDraftIssue(
  fieldKey: String,
  value: Option[String]
)

object FinalValidationDraftIssue {
  given format: Format[FinalValidationDraftIssue] = Json.format[FinalValidationDraftIssue]
}

case class FinalValidationSubcontractorDetails(
  firstName: Option[String] = None,
  secondName: Option[String] = None,
  surname: Option[String] = None,
  partnershipTradingName: Option[String] = None,
  tradingName: Option[String] = None,
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  country: Option[String] = None,
  postcode: Option[String] = None,
  emailAddress: Option[String] = None,
  phoneNumber: Option[String] = None,
  mobilePhoneNumber: Option[String] = None,
  utr: Option[String] = None,
  partnerUtr: Option[String] = None,
  nino: Option[String] = None,
  crn: Option[String] = None,
  worksReferenceNumber: Option[String] = None
)

object FinalValidationSubcontractorDetails {
  given format: Format[FinalValidationSubcontractorDetails] = Json.format[FinalValidationSubcontractorDetails]
}

sealed trait FinalValidationReadiness {
  def key: String
}

object FinalValidationReadiness {

  case object Incomplete extends FinalValidationReadiness {
    override val key: String = "Incomplete"
  }

  case object Complete extends FinalValidationReadiness {
    override val key: String = "Complete"
  }

  given format: Format[FinalValidationReadiness] = new Format[FinalValidationReadiness] {

    override def writes(
      value: FinalValidationReadiness
    ): JsValue =
      JsString(value.key)

    override def reads(
      json: JsValue
    ): JsResult[FinalValidationReadiness] =
      json.validate[String].flatMap {
        case Incomplete.key =>
          JsSuccess(Incomplete)

        case Complete.key =>
          JsSuccess(Complete)

        case other =>
          JsError(s"Unknown FinalValidationReadiness: $other")
      }
  }
}

case class FinalValidationDraftSubcontractor(
  subcontractorId: Long,
  subbieResourceRef: Long,
  baseVersion: Option[Int],
  subcontractorType: Option[String],
  displayName: String,
  base: FinalValidationSubcontractorDetails,
  proposed: FinalValidationSubcontractorDetails,
  changedTargets: Set[String],
  issues: Seq[FinalValidationDraftIssue],
  readiness: FinalValidationReadiness
)

object FinalValidationDraftSubcontractor {
  given format: Format[FinalValidationDraftSubcontractor] = Json.format[FinalValidationDraftSubcontractor]
}

case class FinalValidationDraft(
  subcontractors: Seq[FinalValidationDraftSubcontractor]
) {

  def subcontractor(subcontractorId: Long): Option[FinalValidationDraftSubcontractor] =
    subcontractors.find(_.subcontractorId == subcontractorId)

  def allComplete: Boolean =
    subcontractors.forall(_.readiness == FinalValidationReadiness.Complete)

}

object FinalValidationDraft {
  given format: Format[FinalValidationDraft] = Json.format[FinalValidationDraft]
}

case class CreateFinalValidationDraftSubcontractor(
  subcontractorId: Long,
  subbieResourceRef: Long,
  baseVersion: Option[Int],
  subcontractorType: Option[String],
  displayName: String,
  details: FinalValidationSubcontractorDetails,
  issues: Seq[FinalValidationDraftIssue]
)

object CreateFinalValidationDraftSubcontractor {
  given format: Format[CreateFinalValidationDraftSubcontractor] =
    Json.format[CreateFinalValidationDraftSubcontractor]
}

case class CreateFinalValidationDraftRequest(
  instanceId: String,
  context: String,
  subcontractors: Seq[CreateFinalValidationDraftSubcontractor]
)

object CreateFinalValidationDraftRequest {
  given format: Format[CreateFinalValidationDraftRequest] =
    Json.format[CreateFinalValidationDraftRequest]
}

case class CreateFinalValidationDraftResponse(
  draftId: String
)

object CreateFinalValidationDraftResponse {
  given format: Format[CreateFinalValidationDraftResponse] =
    Json.format[CreateFinalValidationDraftResponse]
}

case class UpdateFinalValidationReadinessRequest(
  subcontractorId: Long,
  issues: Seq[FinalValidationDraftIssue]
)

object UpdateFinalValidationReadinessRequest {
  given format: Format[UpdateFinalValidationReadinessRequest] =
    Json.format[UpdateFinalValidationReadinessRequest]
}
