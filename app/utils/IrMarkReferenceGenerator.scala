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

object IrMarkReferenceGenerator {
  def fromBase64(irMarkBase64: String): String = {
    val decodedBytes = java.util.Base64.getDecoder.decode(irMarkBase64)
    encodeBase32NoPadding(decodedBytes)
  }

  private val base32Alphabet: Array[Char] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray

  private def encodeBase32NoPadding(bytes: Array[Byte]): String = {
    if (bytes.isEmpty) return ""

    val out = new StringBuilder((bytes.length * 8 + 4) / 5)

    var i = 0
    while (i + 4 < bytes.length) {
      val b0 = bytes(i)     & 0xFF
      val b1 = bytes(i + 1) & 0xFF
      val b2 = bytes(i + 2) & 0xFF
      val b3 = bytes(i + 3) & 0xFF
      val b4 = bytes(i + 4) & 0xFF

      out.append(base32Alphabet((b0 >> 3) & 0x1F))
      out.append(base32Alphabet(((b0 << 2) | (b1 >> 6)) & 0x1F))
      out.append(base32Alphabet((b1 >> 1) & 0x1F))
      out.append(base32Alphabet(((b1 << 4) | (b2 >> 4)) & 0x1F))
      out.append(base32Alphabet(((b2 << 1) | (b3 >> 7)) & 0x1F))
      out.append(base32Alphabet((b3 >> 2) & 0x1F))
      out.append(base32Alphabet(((b3 << 3) | (b4 >> 5)) & 0x1F))
      out.append(base32Alphabet(b4 & 0x1F))

      i += 5
    }

    val remaining = bytes.length - i
    if (remaining == 1) {
      val b0 = bytes(i) & 0xFF
      out.append(base32Alphabet((b0 >> 3) & 0x1F))
      out.append(base32Alphabet((b0 << 2) & 0x1F))
    } else if (remaining == 2) {
      val b0 = bytes(i) & 0xFF
      val b1 = bytes(i + 1) & 0xFF
      out.append(base32Alphabet((b0 >> 3) & 0x1F))
      out.append(base32Alphabet(((b0 << 2) | (b1 >> 6)) & 0x1F))
      out.append(base32Alphabet((b1 >> 1) & 0x1F))
      out.append(base32Alphabet((b1 << 4) & 0x1F))
    } else if (remaining == 3) {
      val b0 = bytes(i) & 0xFF
      val b1 = bytes(i + 1) & 0xFF
      val b2 = bytes(i + 2) & 0xFF
      out.append(base32Alphabet((b0 >> 3) & 0x1F))
      out.append(base32Alphabet(((b0 << 2) | (b1 >> 6)) & 0x1F))
      out.append(base32Alphabet((b1 >> 1) & 0x1F))
      out.append(base32Alphabet(((b1 << 4) | (b2 >> 4)) & 0x1F))
      out.append(base32Alphabet((b2 << 1) & 0x1F))
    } else if (remaining == 4) {
      val b0 = bytes(i) & 0xFF
      val b1 = bytes(i + 1) & 0xFF
      val b2 = bytes(i + 2) & 0xFF
      val b3 = bytes(i + 3) & 0xFF
      out.append(base32Alphabet((b0 >> 3) & 0x1F))
      out.append(base32Alphabet(((b0 << 2) | (b1 >> 6)) & 0x1F))
      out.append(base32Alphabet((b1 >> 1) & 0x1F))
      out.append(base32Alphabet(((b1 << 4) | (b2 >> 4)) & 0x1F))
      out.append(base32Alphabet(((b2 << 1) | (b3 >> 7)) & 0x1F))
      out.append(base32Alphabet((b3 >> 2) & 0x1F))
      out.append(base32Alphabet((b3 << 3) & 0x1F))
    }

    out.toString
  }
}
