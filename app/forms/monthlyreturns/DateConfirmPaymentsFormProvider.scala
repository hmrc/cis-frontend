package forms.monthlyreturns

import forms.mappings.Mappings
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class DateConfirmPaymentsFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "dateConfirmPayments.error.invalid",
        allRequiredKey = "dateConfirmPayments.error.required.all",
        twoRequiredKey = "dateConfirmPayments.error.required.two",
        requiredKey    = "dateConfirmPayments.error.required"
      )
    )
}
