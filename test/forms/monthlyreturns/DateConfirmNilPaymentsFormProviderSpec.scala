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

import base.SpecBase
import config.FrontendAppConfig
import forms.behaviours.DateBehaviours
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class DateConfirmNilPaymentsFormProviderSpec extends DateBehaviours with SpecBase with MockitoSugar {

  private implicit val messages: Messages      = stubMessages()
  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"

  private val form = new DateConfirmNilPaymentsFormProvider(mockFrontendAppConfig)()

  ".value" - {

    val validData = oneOf(
      Seq(
        LocalDate.of(2010, 4, 5),
        LocalDate.of(2016, 11, 5),
        LocalDate.of(2018, 12, 5)
      )
    )

    behave like dateField(form, "value", validData)

    behave like mandatoryMonthYearDateField(
      form,
      Seq("value.month", "value.year"),
      Seq(
        "monthlyreturns.dateConfirmNilPayments.error.required.month",
        "monthlyreturns.dateConfirmNilPayments.error.required.year"
      )
    )
  }
}
