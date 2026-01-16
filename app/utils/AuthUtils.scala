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

package utils

import models.EmployerReference
import play.api.Logging
import uk.gov.hmrc.auth.core.Enrolment

object AuthUtils extends Logging {

  def hasCisOrgEnrolment[A](enrolments: Set[Enrolment]): Option[EmployerReference] =
    enrolments.find(_.key == "HMRC-CIS-ORG") match {
      case Some(enrolment) =>
        val taxOfficeNumber    = enrolment.identifiers.find(id => id.key == "TaxOfficeNumber").map(_.value)
        val taxOfficeReference = enrolment.identifiers.find(id => id.key == "TaxOfficeReference").map(_.value)
        val isActivated        = enrolment.isActivated
        (taxOfficeNumber, taxOfficeReference, isActivated) match {
          case (Some(number), Some(reference), true) if isValidTaxOfficeNumber(number) =>
            Some(EmployerReference(number, reference))
          case _                                                                       =>
            logger.warn("EnrolmentAuthIdentifierAction - Unable to retrieve activated cis enrolments")
            None
        }
      case _               => None
    }

  def hasCisAgentEnrolment[A](enrolments: Set[Enrolment]): Option[String] =
    enrolments.find(_.key == "IR-PAYE-AGENT") match {
      case Some(enrolment) =>
        val irAgentReference = enrolment.identifiers.find(id => id.key == "IRAgentReference").map(_.value)
        val isActivated      = enrolment.isActivated
        (irAgentReference, isActivated) match {
          case (Some(reference), true) if isValidAgentReference(reference) =>
            Some(reference)
          case _                                                           =>
            logger.warn("EnrolmentAuthIdentifierAction - Unable to retrieve activated agent reference")
            None
        }
      case _               => None
    }

  def isValidTaxOfficeNumber(taxOfficeNumber: String): Boolean =
    (taxOfficeNumber != null) && taxOfficeNumber.matches("[0-9]{3}")

  def isValidAgentReference(agentReference: String): Boolean =
    (agentReference != null) && agentReference.matches("[A-Za-z0-9]{6}")

}
