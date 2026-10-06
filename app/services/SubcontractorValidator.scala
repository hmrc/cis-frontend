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

package services

import models.monthlyreturns.Subcontractor
import models.validation.*
import models.submission.SubcontractorType

import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class SubcontractorValidator @Inject() (
  subcontractorDetailsValidator: SubcontractorDetailsValidator,
  subcontractorCompanyValidator: SubcontractorCompanyValidator,
  subcontractorIndividualValidator: SubcontractorIndividualValidator,
  subcontractorPartnershipValidator: SubcontractorPartnershipValidator,
  subcontractorTrustValidator: SubcontractorTrustValidator
) {

  def validate(subcontractors: Seq[Subcontractor]): List[SubcontractorValidationFailure] = {

    validateSubcontractorTypes(subcontractors)

    SubcontractorValidationFailure.merge(
      subcontractorDetailsValidator.validate(subcontractors),
      subcontractorCompanyValidator.validate(subcontractors),
      subcontractorIndividualValidator.validate(subcontractors),
      subcontractorPartnershipValidator.validate(subcontractors),
      subcontractorTrustValidator.validate(subcontractors)
    )
  }

  def validateFields(
    subcontractors: Seq[Subcontractor]
  ): Map[Long, Seq[SubcontractorValidationField]] =
    validate(subcontractors).map { failure =>
      failure.subcontractorId ->
        failure.failedFields
          .map(_.field)
          .distinct
    }.toMap

  def validateFieldsFor(
    subcontractorId: Long,
    subcontractors: Seq[Subcontractor]
  ): Seq[SubcontractorValidationField] =
    validateFields(subcontractors)
      .getOrElse(subcontractorId, Seq.empty)

  private def validateSubcontractorTypes(
    subcontractors: Seq[Subcontractor]
  ): Unit =
    subcontractors.foreach { subcontractor =>
      val validType =
        subcontractor.subcontractorType
          .flatMap { value =>
            Try(
              SubcontractorType.fromString(value)
            ).toOption
          }

      if (validType.isEmpty) {
        throw new IllegalStateException(s"Invalid subcontractorType: ${subcontractor.subcontractorType}")
      }
    }
}
