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

package forms.monthlyreturns

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class DateConfirmPaymentsFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      mapping(
        "taxMonth" -> int(
          requiredKey = "dateConfirmPayments.taxMonth.error.required",
          wholeNumberKey = "dateConfirmPayments.taxMonth.error.wholeNumber",
          nonNumericKey = "dateConfirmPayments.taxMonth.error.nonNumeric"
        ).verifying("dateConfirmPayments.taxMonth.error.range", month => month >= 1 && month <= 12),
        "taxYear"  -> int(
          requiredKey = "dateConfirmPayments.taxYear.error.required",
          wholeNumberKey = "dateConfirmPayments.taxYear.error.wholeNumber",
          nonNumericKey = "dateConfirmPayments.taxYear.error.nonNumeric"
        ).verifying("dateConfirmPayments.taxYear.error.range", year => year >= 2000 && year <= 9999)
      )((month, year) => LocalDate.of(year, month, 1))(date => Some((date.getMonthValue, date.getYear)))
    )
}
