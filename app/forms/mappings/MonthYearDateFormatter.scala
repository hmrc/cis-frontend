/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms.mappings

import config.FrontendAppConfig
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, YearMonth}
import java.util.Locale
import javax.inject.Inject
import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.{Failure, Success, Try}

case class DateFormat(dateType: String, errorKey: String, regex: String)

class MonthYearDateFormatter @Inject() (
  invalidKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  args: Seq[String] = Seq.empty,
  dateFormats: Seq[DateFormat],
  fieldKeys: Seq[String],
  config: FrontendAppConfig
)(implicit messages: Messages)
    extends LocalDateFormatter(invalidKey, "", twoRequiredKey, requiredKey, args) {

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val fields: Map[String, Option[String]] = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    lazy val missingFieldErrors = fields.collect { case (field, None) =>
      FormError(s"$key.$field", s"monthlyreturns.dateConfirmNilPayments.error.required.$field")
    }.toList

    lazy val regexErrors = dateFormats.flatMap(checkInput(key, fields, _))

    lazy val earliestMonthYearDateErrors = earliestMonthYearDateCheck(fields)

    lazy val maxMonthYearDateErrors = maxMonthYearDateCheck(fields)

    if missingFieldErrors.nonEmpty || regexErrors.nonEmpty then Left(missingFieldErrors ++ regexErrors)
    else if earliestMonthYearDateErrors.nonEmpty then Left(earliestMonthYearDateErrors.toSeq)
    else if maxMonthYearDateErrors.nonEmpty then Left(maxMonthYearDateErrors.toSeq)
    else formatDate(key, data).left.map(_.map(_.copy(key = key, args = args)))
  }

  private def checkInput(key: String, fields: Map[String, Option[String]], dateFormat: DateFormat): Option[FormError] =
    fields.get(dateFormat.dateType).flatten match {
      case Some(dateType) if !dateType.matches(dateFormat.regex) =>
        Some(FormError(s"$key.${dateFormat.dateType}", dateFormat.errorKey, args))
      case _                                                     =>
        None
    }

  private def earliestMonthYearDateCheck(fields: Map[String, Option[String]]): Option[FormError] = {

    val earliestTaxPeriodEndDate: LocalDate = LocalDate.parse(config.earliestTaxPeriodEndDate)

    val formattedDateInSeq =
      earliestTaxPeriodEndDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)).split(" ").toSeq

    (fields.get("month").flatten, fields.get("year").flatten) match {
      case (Some(month), Some(year))
          if !((
            year.toInt,
            month.toInt
          ) >= (earliestTaxPeriodEndDate.getYear, earliestTaxPeriodEndDate.getMonthValue)) =>
        Some(
          FormError(
            "value.month",
            "monthlyreturns.dateConfirmNilPayments.error.invalid.earliestTaxPeriodEndDate",
            formattedDateInSeq
          )
        )
      case _ =>
        None

    }
  }

  private def maxMonthYearDateCheck(fields: Map[String, Option[String]]): Option[FormError] = {

    val oMonth = fields.get("month").flatten
    val oYear  = fields.get("year").flatten

    val today              = LocalDate.now()
    val maxDate: LocalDate = if (today.getDayOfMonth <= 5) today.plusMonths(3) else today.plusMonths(4)

    (oMonth, oYear) match {
      case (Some(month), Some(year))
          if YearMonth.from(LocalDate.of(year.toInt, month.toInt, 5)).isAfter(YearMonth.from(maxDate)) =>
        Some(
          FormError("value.month", "monthlyreturns.dateConfirmNilPayments.error.invalid.maxAllowedFutureReturnPeriod")
        )
      case _ =>
        None

    }
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val int = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey,
      args
    )

    val month = new MonthFormatter(invalidKey, args)

    for {
      month <- month.bind(s"$key.month", data)
      year  <- int.bind(s"$key.year", data)
      date  <- toDate(key, month, year)
    } yield date
  }

  private def toDate(key: String, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, 5)) match {
      case Success(date) =>
        Right(date)
      case Failure(_)    =>
        Left(Seq(FormError(key, invalidKey, args)))
    }

}
