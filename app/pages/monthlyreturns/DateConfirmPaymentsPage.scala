package pages.monthlyreturns

import pages.QuestionPage
import play.api.libs.json.JsPath

import java.time.LocalDate

case object DateConfirmPaymentsPage extends QuestionPage[LocalDate] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "dateConfirmPayments"
}
