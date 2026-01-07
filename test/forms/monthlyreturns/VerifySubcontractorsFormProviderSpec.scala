package forms.monthlyreturns

import forms.behaviours.OptionFieldBehaviours
import forms.monthlyreturns.VerifySubcontractorsFormProvider
import models.VerifySubcontractors
import play.api.data.FormError

class VerifySubcontractorsFormProviderSpec extends OptionFieldBehaviours {

  val form = new VerifySubcontractorsFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "verifySubcontractors.error.required"

    behave like optionsField[VerifySubcontractors](
      form,
      fieldName,
      validValues = VerifySubcontractors.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
