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

import models.Enumerable
import play.api.data.FormError
import play.api.data.format.Formatter

import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private[mappings] def stringFormatter(errorKey: String, args: Seq[String] = Seq.empty): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None                      => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) if s.trim.isEmpty => Left(Seq(FormError(key, errorKey, args)))
          case Some(s)                   => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case _       => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  private[mappings] def booleanDefaultFalseFormatter(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Boolean] = new Formatter[Boolean] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
      data.get(key) match {
        case Some("true") => Right(true)
        case _            => Right(false)
      }

    def unbind(key: String, value: Boolean) = Map(key -> value.toString)
  }

  private[mappings] def seqFormatter[A](using baseFormatter: Formatter[A]): Formatter[Seq[A]] =
    new Formatter[Seq[A]] {

      override def bind(keyPrefix: String, data: Map[String, String]): Either[List[FormError], Seq[A]] =
        data
          .filter((key, _) => key.matches(s"^$keyPrefix\\.\\d+"))
          .toList
          .sortBy((key, _) => key)
          .map((key, _) => baseFormatter.bind(key, data))
          .partitionMap(identity) match {
          case (Nil, rights) => Right(rights)
          case (lefts, _)    => Left(lefts.flatten)
        }

      def unbind(key: String, values: Seq[A]): Map[String, String] =
        values.zipWithIndex
          .map((value, index) => s"$key.$index" -> value.toString)
          .toMap
    }

  private[mappings] def intFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s                             =>
              nonFatalCatch
                .either(s.toInt)
                .left
                .map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: Int) =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty)(
    implicit ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.withName(str)
            .map(Right.apply)
            .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def currencyFormatter(
    requiredKey: String,
    invalidNumericKey: String,
    nonNumericKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      val isNumeric    = """(^£?\d*$)|(^£?\d*\.\d*$)"""
      val validDecimal = """(^£?\d*$)|(^£?\d*\.\d{1,2}$)"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", "").replace(" ", ""))
          .flatMap {
            case s if !s.matches(isNumeric)    =>
              Left(Seq(FormError(key, nonNumericKey, args)))
            case s if !s.matches(validDecimal) =>
              Left(Seq(FormError(key, invalidNumericKey, args)))
            case s                             =>
              nonFatalCatch
                .either(BigDecimal(s.replace("£", "")))
                .left
                .map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def paymentDetailsCurrencyFormatter(
    requiredKey: String,
    invalidKey: String,
    maxLengthKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      val paymentDetailsRegex = """^[0-9,]+[.]{0,1}[0-9]{0,2}$"""
      val maxLength           = 13

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .flatMap { rawInput =>
            val input = rawInput.trim
            if (input.length > maxLength) {
              Left(Seq(FormError(key, maxLengthKey, args)))
            } else if (!input.matches(paymentDetailsRegex)) {
              Left(Seq(FormError(key, invalidKey, args)))
            } else {
              val cleaned = input.replace(",", "")
              nonFatalCatch
                .either(BigDecimal(cleaned))
                .left
                .map(_ => Seq(FormError(key, invalidKey, args)))
                .flatMap { value =>
                  if (value % 1 != 0) {
                    Left(Seq(FormError(key, invalidKey, args)))
                  } else {
                    Right(value)
                  }
                }
            }
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def taxDeductedCurrencyFormatter(
    requiredKey: String,
    invalidKey: String,
    maxLengthKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      val taxDeductedRegex = """^[0-9,]+(\.[0-9]{1,2})?$"""
      val maxLength        = 13

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .flatMap { rawInput =>
            val input = rawInput.trim
            if (input.length > maxLength) {
              Left(Seq(FormError(key, maxLengthKey, args)))
            } else if (!input.matches(taxDeductedRegex)) {
              Left(Seq(FormError(key, invalidKey, args)))
            } else {
              val cleaned = input.replace(",", "")
              nonFatalCatch
                .either(BigDecimal(cleaned))
                .left
                .map(_ => Seq(FormError(key, invalidKey, args)))
            }
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }
}
