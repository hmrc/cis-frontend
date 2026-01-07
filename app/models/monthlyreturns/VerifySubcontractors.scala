package models.monthlyreturns

import models.{Enumerable, VerifySubcontractors, WithName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait VerifySubcontractors

object VerifySubcontractors extends Enumerable.Implicits {

  case object Yes extends WithName("yes") with VerifySubcontractors
  case object No extends WithName("no") with VerifySubcontractors

  val values: Seq[VerifySubcontractors] = Seq(
    Yes, No
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"verifySubcontractors.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[VerifySubcontractors] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
