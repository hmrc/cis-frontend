package forms

import forms.behaviours.BooleanFieldBehaviours
import forms.monthlyreturns.SubmitInactivityRequestFormProvider
import play.api.data.FormError

class SubmitInactivityRequestFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "submitInactivityRequest.error.required"
  val invalidKey  = "error.boolean"

  val form = new SubmitInactivityRequestFormProvider()()

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
