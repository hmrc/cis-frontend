package forms.history

import forms.behaviours.OptionFieldBehaviours
import forms.history.SubmittedReturnsChooseTaxYearFormProvider
import models.history.SubmittedReturnsChooseTaxYear
import play.api.data.FormError

class SubmittedReturnsChooseTaxYearFormProviderSpec extends OptionFieldBehaviours {

  val form = new SubmittedReturnsChooseTaxYearFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "history.submittedReturnsChooseTaxYear.error.required"

    behave like optionsField[SubmittedReturnsChooseTaxYear](
      form,
      fieldName,
      validValues  = SubmittedReturnsChooseTaxYear.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
