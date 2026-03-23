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

import models.UserAnswers
import models.monthlyreturns.UpdateMonthlyReturnItemRequest
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import utils.MoneyFormat

import javax.inject.{Inject, Singleton}

trait MonthlyReturnItemPayloadBuilder {
  def build(ua: UserAnswers, index: Int): Option[UpdateMonthlyReturnItemRequest]
}

@Singleton
class MonthlyReturnItemPayloadBuilderImpl @Inject() () extends MonthlyReturnItemPayloadBuilder {

  override def build(ua: UserAnswers, index: Int): Option[UpdateMonthlyReturnItemRequest] =
    for {
      instanceId    <- ua.get(CisIdPage)
      monthDate     <- ua.get(DateConfirmPaymentsPage)
      subcontractor <- ua.get(SelectedSubcontractorPage(index))
      totalPayments <- subcontractor.totalPaymentsMade
    } yield {
      val costOfMaterials  = subcontractor.costOfMaterials.getOrElse(BigDecimal(0))
      val totalTaxDeducted = subcontractor.totalTaxDeducted.getOrElse(BigDecimal(0))

      UpdateMonthlyReturnItemRequest(
        instanceId = instanceId,
        taxYear = monthDate.getYear,
        taxMonth = monthDate.getMonthValue,
        subcontractorId = subcontractor.id,
        subcontractorName = subcontractor.name,
        totalPayments = MoneyFormat.twoDp(totalPayments),
        costOfMaterials = MoneyFormat.twoDp(costOfMaterials),
        totalDeducted = MoneyFormat.twoDp(totalTaxDeducted)
      )
    }
}
