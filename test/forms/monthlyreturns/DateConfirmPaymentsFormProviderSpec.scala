package forms.monthlyreturns

import forms.behaviours.DateBehaviours
import forms.monthlyreturns.DateConfirmPaymentsFormProvider
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.{LocalDate, ZoneOffset}

class DateConfirmPaymentsFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val form                        = new DateConfirmPaymentsFormProvider()()

  ".value" - {

    val validData = datesBetween(
      min = LocalDate.of(2000, 1, 1),
      max = LocalDate.now(ZoneOffset.UTC)
    )

    behave like dateField(form, "value", validData)

    behave like mandatoryDateField(form, "value", "dateConfirmPayments.error.required.all")
  }
}
