package forms

import forms.behaviours.OptionFieldBehaviours
import models.AeYouSureYouWantToAmend
import play.api.data.FormError

class AeYouSureYouWantToAmendFormProviderSpec extends OptionFieldBehaviours {

  val form = new AeYouSureYouWantToAmendFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "aeYouSureYouWantToAmend.error.required"

    behave like optionsField[AeYouSureYouWantToAmend](
      form,
      fieldName,
      validValues  = AeYouSureYouWantToAmend.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
