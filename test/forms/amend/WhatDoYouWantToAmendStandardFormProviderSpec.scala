package forms.amend

import forms.behaviours.OptionFieldBehaviours
import models.amend.WhatDoYouWantToAmendStandard
import play.api.data.FormError

class WhatDoYouWantToAmendStandardFormProviderSpec extends OptionFieldBehaviours {

  val form = new WhatDoYouWantToAmendStandardFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "amend.whatDoYouWantToAmendStandard.error.required"

    behave like optionsField[WhatDoYouWantToAmendStandard](
      form,
      fieldName,
      validValues = WhatDoYouWantToAmendStandard.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
