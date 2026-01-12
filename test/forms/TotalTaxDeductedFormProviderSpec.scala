package forms

import forms.behaviours.IntFieldBehaviours
import forms.monthlyreturns.TotalTaxDeductedFormProvider
import play.api.data.FormError

class TotalTaxDeductedFormProviderSpec extends IntFieldBehaviours {

  val form = new TotalTaxDeductedFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = 99999999.99

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "totalTaxDeducted.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "totalTaxDeducted.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "totalTaxDeducted.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "totalTaxDeducted.error.required")
    )
  }
}
