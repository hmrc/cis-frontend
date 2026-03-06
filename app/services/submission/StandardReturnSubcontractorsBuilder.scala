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

package services.submission

import models.UserAnswers
import models.submission.{ChrisPersonName, ChrisStandardSubcontractor, SubcontractorType}
import models.monthlyreturns.{SelectedSubcontractor, Subcontractor}
import models.submission.SubcontractorType.*
import pages.monthlyreturns.*
import utils.Normalise.{isBLank, nonBlank}

object StandardReturnSubcontractorsBuilder {

  def build(ua: UserAnswers, allSubs: Seq[Subcontractor]): Seq[ChrisStandardSubcontractor] = {
    val selected: Seq[SelectedSubcontractor] =
      ua.get(SelectedSubcontractorPage.all)
        .toSeq
        .flatMap(_.toSeq)
        .sortBy(_._1)
        .map(_._2)

    if (selected.isEmpty) throw new RuntimeException("No subcontractors selected for standard return submission")

    val subsById: Map[Long, Subcontractor] = allSubs.map(sub => sub.subcontractorId -> sub).toMap

    selected.map { selectedSub =>
      val sub = subsById.getOrElse(
        selectedSub.id,
        throw new RuntimeException(
          s"No subcontractor found with id ${selectedSub.id} (selected name='${selectedSub.name}')"
        )
      )

      val subType = SubcontractorType.fromString(
        sub.subcontractorType.getOrElse(throw new RuntimeException("Missing subcontractor type"))
      )

      ChrisStandardSubcontractor(
        subcontractorType = subType,
        name = buildPersonName(sub, subType),
        tradingName = nonBlank(sub.tradingName),
        partnershipTradingName = nonBlank(sub.partnershipTradingName),
        utr = nonBlank(sub.utr),
        crn = nonBlank(sub.crn),
        nino = nonBlank(sub.nino),
        verificationNumber = nonBlank(sub.verificationNumber),
        totalPayments = selectedSub.totalPaymentsMade,
        costOfMaterials = selectedSub.costOfMaterials,
        totalDeducted = selectedSub.totalTaxDeducted
      )
    }
  }

  private def buildPersonName(
    sub: Subcontractor,
    subType: SubcontractorType
  ): Option[ChrisPersonName] =
    if (subType == SubcontractorType.SoleTrader && isBLank(sub.tradingName)) {
      for {
        firstName <- nonBlank(sub.firstName)
        lastName  <- nonBlank(sub.surname)
      } yield ChrisPersonName(
        first = firstName,
        middle = nonBlank(sub.secondName),
        last = lastName
      )
    } else {
      None
    }
}
