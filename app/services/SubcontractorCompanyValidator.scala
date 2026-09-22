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

import models.validation.SubcontractorValidationFailure
import models.monthlyreturns.Subcontractor
import models.submission.SubcontractorType
import utils.CompanyValidator

import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class SubcontractorCompanyValidator @Inject() {
  def validate(
    subcontractors: Seq[Subcontractor]
  ): List[SubcontractorValidationFailure] =
    subcontractors.toList
      .filter(isCompany)
      .flatMap { subcontractor =>
        val failedFields =
          CompanyValidator.validate(
            subcontractor = subcontractor,
            subcontractors = subcontractors
          )

        Option.when(failedFields.nonEmpty) {
          SubcontractorValidationFailure(
            subcontractorId = subcontractor.subcontractorId,
            failedFields = failedFields
          )
        }
      }

  private def isCompany(
    subcontractor: Subcontractor
  ): Boolean =
    subcontractor.subcontractorType
      .flatMap { value =>
        Try(SubcontractorType.fromString(value)).toOption
      }
      .contains(SubcontractorType.Company)
}
