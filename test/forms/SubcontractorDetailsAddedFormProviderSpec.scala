package forms

import forms.behaviours.BooleanFieldBehaviours
import forms.monthlyreturns.SubcontractorDetailsAddedFormProvider
import play.api.data.FormError

class SubcontractorDetailsAddedFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "subcontractorDetailsAdded.error.required"
  val invalidKey = "error.boolean"

  val form = new SubcontractorDetailsAddedFormProvider()()

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
