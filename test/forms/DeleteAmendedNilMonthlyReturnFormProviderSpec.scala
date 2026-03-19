package forms

import forms.behaviours.BooleanFieldBehaviours
import forms.monthlyreturns.DeleteAmendedNilMonthlyReturnFormProvider
import play.api.data.FormError

class DeleteAmendedNilMonthlyReturnFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "deleteAmendedNilMonthlyReturn.error.required"
  val invalidKey = "error.boolean"

  val form = new DeleteAmendedNilMonthlyReturnFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
