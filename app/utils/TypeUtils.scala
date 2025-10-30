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

package utils

import scala.concurrent.Future
import scala.util.Try

object TypeUtils {
  extension [T](o: Option[T]) {
    def toTry(message: String): Try[T]       = o.toRight(new Exception(message)).toTry
    def toTry: Try[T]                        = o.toTry("No value found")
    def toFuture(message: String): Future[T] = Future.fromTry(o.toTry(message))
    def toFuture: Future[T]                  = Future.fromTry(o.toTry)
  }

  extension [T](t: Try[T]) {
    def toFuture: Future[T] = Future.fromTry(t)
  }
}
