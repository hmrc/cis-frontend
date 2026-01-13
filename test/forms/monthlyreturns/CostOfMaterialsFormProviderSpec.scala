package forms.monthlyreturns

import utils.CurrencyFormatter.currencyFormat
import forms.behaviours.CurrencyFieldBehaviours
import forms.monthlyreturns.CostOfMaterialsFormProvider
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class CostOfMaterialsFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new CostOfMaterialsFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = BigDecimal("0")
    val maximum = BigDecimal("99999999.00")

    val validDataGenerator =
      Gen
        .choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "monthlyreturns.costOfMaterials.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "monthlyreturns.costOfMaterials.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "monthlyreturns.costOfMaterials.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "monthlyreturns.costOfMaterials.error.required")
    )
  }
}
