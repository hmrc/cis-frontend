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

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.Locale
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

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case _       => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
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

    def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
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

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
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

      override def unbind(key: String, value: Int): Map[String, String] =
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
    invalidKey: String,
    maxLengthKey: String,
    scale: Int, // 0 or 2
    args: Seq[String] = Seq.empty
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {

      require(scale == 0 || scale == 2, s"Unsupported scale: $scale (expected 0 or 2)")

      private val maxLength     = 16
      private val baseFormatter = stringFormatter(requiredKey, args)

      // Accept digits and commas in the integer part; no comma-grouping validation.
      private val intPartWithOptionalCommas = """[0-9,]+"""

      private val decimalsForScale: String =
        scale match {
          case 0 =>
            // allow: integer, ".", ".0", ".00"
            """(?:\.(?:0|00)?)?"""
          case 2 =>
            // allow: integer, ".", ".<1-2 digits>"
            """(?:\.(?:\d{1,2})?)?"""
        }

      // Allow optional £ and spaces anywhere (we strip spaces before matching by normalising first)
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .flatMap { rawInput =>
            val input = rawInput.trim

            if (input.isEmpty) {
              Left(Seq(FormError(key, requiredKey, args)))
            } else if (input.length > maxLength) {
              Left(Seq(FormError(key, maxLengthKey, args)))
            } else {

              // Normalise FIRST (strip spaces + £) so we can allow inputs like "£ 1,234 . 56"
              val normalisedForValidation =
                input.filterNot(_.isWhitespace)

              val allowedRawPattern =
                ("^£?" + intPartWithOptionalCommas + decimalsForScale + "$").r

              if (allowedRawPattern.findFirstIn(normalisedForValidation).isEmpty) {
                Left(Seq(FormError(key, invalidKey, args)))
              } else {
                val cleaned0 =
                  normalisedForValidation
                    .replace("£", "")
                    .replace(",", "")

                // Normalise trailing dot:
                // "100." -> "100"
                // "0."   -> "0"
                val cleaned =
                  if (cleaned0.endsWith(".")) cleaned0.dropRight(1) else cleaned0

                val parts          = cleaned.split("\\.", -1)
                val hasDecimalPart = parts.length == 2
                val decPart        = if (hasDecimalPart) parts(1) else ""

                // For scale=0: only allow decimal part "", "0", "00" ("" happens for trailing dot)
                val decimalNotAllowedForScale0 =
                  scale == 0 && hasDecimalPart && !(decPart.isEmpty || decPart == "0" || decPart == "00")

                if (decimalNotAllowedForScale0) {
                  Left(Seq(FormError(key, invalidKey, args)))
                } else {
                  nonFatalCatch
                    .either(BigDecimal(cleaned))
                    .left
                    .map(_ => Seq(FormError(key, invalidKey, args)))
                    .flatMap { value =>
                      val scaleOk =
                        scale match {
                          case 0 => value.scale <= 0 || value.setScale(0) == value
                          case 2 => value.scale <= 2
                        }

                      if (!scaleOk) Left(Seq(FormError(key, invalidKey, args)))
                      else Right(value)
                    }
                }
              }
            }
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] = {
        val symbols = DecimalFormatSymbols.getInstance(Locale.UK)

        val rendered =
          scale match {
            case 0 =>
<<<<<<< Updated upstream
              val df = new DecimalFormat("#,##0", symbols)
              df.format(value.setScale(0).bigDecimal)

=======
//              // PaymentDetails & CostOfMaterials (if not null): always show whole pounds with commas, no decimals
//              val df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.UK))
//              df.format(value.setScale(0).bigDecimal)
              // PaymentDetails: always show whole pounds, no decimals
              value.setScale(0).toBigIntExact.map(_.toString).getOrElse(value.setScale(0).toString)
>>>>>>> Stashed changes
            case 2 =>
              val df = new DecimalFormat("#,##0.00", symbols)
              df.format(value.setScale(2).bigDecimal)
          }

        baseFormatter.unbind(key, rendered)
      }

    }

}
