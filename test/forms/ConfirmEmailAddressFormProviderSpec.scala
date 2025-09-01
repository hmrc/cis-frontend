package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class ConfirmEmailAddressFormProviderSpec extends StringFieldBehaviours {

  private val EmailRegex = "^[A-Za-z0-9!#$%&*+-/=?^_`{|}~.]+@[A-Za-z0-9!#$%&*+-/=?^_`{|}~.]+$"
  private val MaxEmailLength = 254

  val requiredKey = "confirmEmailAddress.error.required"
  val lengthKey = "confirmEmailAddress.error.length"
  val maxLength = MaxEmailLength
  val invalidKey = "confirmEmailAddress.error.invalid"

  val form = new ConfirmEmailAddressFormProvider()()

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

    "must reject invalid email formats" in {
      val invalidEmails = Seq(
        "invalid-email",
        "@domain.com",
        "user@",
        "user@.com",
        "user..name@domain.com",
        "user@domain..com",
        "user name@domain.com",
        "user@domain com"
      )

      invalidEmails.foreach { invalidEmail =>
        val result = form.bind(Map(fieldName -> invalidEmail))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "must accept valid email formats" in {
      val validEmails = Seq(
        "user@domain.com",
        "user.name@domain.com",
        "user+tag@domain.com",
        "user@domain.co.uk",
        "user123@domain123.com",
        "user!#$%&*+-/=?^_`{|}~@domain.com",
        "user@domain!#$%&*+-/=?^_`{|}~.com"
      )

      validEmails.foreach { validEmail =>
        val result = form.bind(Map(fieldName -> validEmail))
        result.errors must not contain FormError(fieldName, invalidKey)
      }
    }
  }
}
