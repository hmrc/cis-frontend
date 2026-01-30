package forms.monthlyreturns

import forms.behaviours.StringFieldBehaviours
import forms.monthlyreturns.EnterYourEmailAddressFormProvider
import play.api.data.FormError

class EnterYourEmailAddressFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "enterYourEmailAddress.error.required"
  val lengthKey = "enterYourEmailAddress.error.length"
  val maxLength = 132

  val form = new EnterYourEmailAddressFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
