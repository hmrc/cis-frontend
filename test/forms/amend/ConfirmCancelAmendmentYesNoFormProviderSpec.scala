package forms.amend

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class ConfirmCancelAmendmentYesNoFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "amend.confirmCancelAmendmentYesNo.error.required"
  val invalidKey  = "error.boolean"

  val form = new ConfirmCancelAmendmentYesNoFormProvider()()

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
