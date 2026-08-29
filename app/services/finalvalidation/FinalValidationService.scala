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

package services.finalvalidation

import models.UserAnswers
import models.finalvalidation.{FinalValidationField, FinalValidationIssue, FinalValidationResult, SubcontractorFinalValidationFailure}
import models.monthlyreturns.Subcontractor
import models.submission.SubcontractorType
import models.submission.SubcontractorType.{Company, Partnership, SoleTrader, Trust}
import pages.finalvalidations.FinalValidationErrorPage
import play.api.Logging
import repositories.SessionRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class FinalValidationService @Inject() (
  companySubcontractorFinalValidation: CompanySubcontractorFinalValidation,
  individualSubcontractorFinalValidation: IndividualSubcontractorFinalValidation,
  trustSubcontractorFinalValidation: TrustSubcontractorFinalValidation,
  partnershipSubcontractorFinalValidation: PartnershipSubcontractorFinalValidation,
  addressDetailsFinalValidation: AddressDetailsFinalValidation,
  sessionRepository: SessionRepository
)(using ec: ExecutionContext)
    extends Logging {

  def validateAndStore(
    userAnswers: UserAnswers,
    selectedSubcontractors: Seq[Subcontractor],
    allSubcontractors: Seq[Subcontractor]
  ): Future[FinalValidationResult] = {

    logger.info(
      s"[FinalValidationService] Starting FinalValidation for selected subcontractors: " +
        s"${selectedSubcontractors.map(_.subcontractorId).mkString(",")}"
    )

    val failures =
      selectedSubcontractors.flatMap { subcontractor =>

        val issues = finalValidationIssues(subcontractor, allSubcontractors)

        if (issues.nonEmpty) {
          Some(
            SubcontractorFinalValidationFailure(
              subcontractorId = subcontractor.subcontractorId,
              issues = issues,
              subbieResourceRef = subcontractor.subbieResourceRef
            )
          )
        } else {
          None
        }
      }

    logger.info(
      s"[FinalValidationService] FinalValidation completed. " +
        s"Failures: ${failures.map(_.subcontractorId).mkString(",")}"
    )

    val result = FinalValidationResult(failures = failures)

    for {
      updatedAnswers <- Future.fromTry {
                          if (result.hasErrors) {
                            userAnswers.set(FinalValidationErrorPage, result.failures)
                          } else {
                            userAnswers.remove(FinalValidationErrorPage)
                          }
                        }
      stored         <- sessionRepository.set(updatedAnswers)
      _              <- if (stored) {
                          Future.unit
                        } else {
                          Future.failed(new RuntimeException("Failed to persist FinalValidation state"))
                        }
    } yield result
  }

  private def finalValidationIssues(
    subcontractor: Subcontractor,
    allSubcontractors: Seq[Subcontractor]
  ): Seq[FinalValidationIssue] =
    finalValidationFields(subcontractor, allSubcontractors).distinct
      .map { field =>
        FinalValidationIssue(
          field = field,
          value = field.valueFrom(subcontractor)
        )
      }

  private def finalValidationFields(
    subcontractor: Subcontractor,
    allSubcontractors: Seq[Subcontractor]
  ): Seq[FinalValidationField] = {

    val subcontractorType =
      subcontractor.subcontractorType
        .flatMap { value =>
          Try(SubcontractorType.fromString(value)).toOption
        }

    subcontractorType match {

      case Some(SoleTrader) =>
        logger.info(
          s"[FinalValidationService] Running Individual + Address FinalValidation " +
            s"for subcontractorId=${subcontractor.subcontractorId}"
        )
        val subcontractorFields =
          individualSubcontractorFinalValidation.validate(subcontractor, allSubcontractors)
        val addressFields       = addressDetailsFinalValidation.validate(subcontractor)
        subcontractorFields ++ addressFields

      case Some(Company) =>
        logger.info(
          s"[FinalValidationService] Running Company + Address FinalValidation " +
            s"for subcontractorId=${subcontractor.subcontractorId}"
        )
        val subcontractorFields =
          companySubcontractorFinalValidation.validate(subcontractor, allSubcontractors)
        val addressFields       = addressDetailsFinalValidation.validate(subcontractor)
        subcontractorFields ++ addressFields

      case Some(Trust) =>
        logger.info(
          s"[FinalValidationService] Running Trust + Address FinalValidation " +
            s"for subcontractorId=${subcontractor.subcontractorId}"
        )
        val subcontractorFields =
          trustSubcontractorFinalValidation.validate(subcontractor, allSubcontractors)
        val addressFields       = addressDetailsFinalValidation.validate(subcontractor)
        subcontractorFields ++ addressFields

      case Some(Partnership) =>
        logger.info(
          s"[FinalValidationService] Running Partnership + Address FinalValidation " +
            s"for subcontractorId=${subcontractor.subcontractorId}"
        )
        val subcontractorFields =
          partnershipSubcontractorFinalValidation.validate(subcontractor, allSubcontractors)
        val addressFields       = addressDetailsFinalValidation.validate(subcontractor)
        subcontractorFields ++ addressFields

      case None =>
        throw new IllegalArgumentException(
          s"Unknown subcontractor type for subcontractor ID: ${subcontractor.subcontractorId}"
        )
    }
  }
}
