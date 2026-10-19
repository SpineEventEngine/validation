/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.validation.java.generate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import javax.lang.model.SourceVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Top-level functions in `JavaIdentifiers.kt` should")
internal class JavaIdentifiersSpec {

    @Nested inner class
    `provide 'mangled()' that` {

        @Test
        fun `appends the hash of the identifier`() {
            // The expected hash is computed independently of the implementation.
            // The URL-safe Base64 encoding of the first 10 bytes of the SHA-256 digest
            // of `fooPattern` is `JfU6l-QD8acWPQ`, and `-` is not a Java identifier char.
            mangled("fooPattern") shouldBe "fooPattern_JfU6lQD8acWPQ"
        }

        @Test
        fun `yields the same name for the same identifier`() {
            mangled("validateNames") shouldBe mangled("validateNames")
        }

        @Test
        fun `yields different hashes for different identifiers`() {
            val fooHash = mangled("foo").removePrefix("foo_")
            val barHash = mangled("bar").removePrefix("bar_")

            fooHash shouldNotBe barHash
        }

        @Test
        fun `yields valid Java identifiers`() {
            // Some of these identifiers, such as `field2`, have `-` in the encoded digest.
            // That char must be filtered out.
            val identifiers = (1..100).map { "field$it" }

            identifiers.forEach { identifier ->
                val name = mangled(identifier)
                name shouldStartWith "${identifier}_"
                SourceVersion.isIdentifier(name) shouldBe true
            }
        }

        @Test
        fun `encodes the given number of digest bytes`() {
            // Base64 encodes every 3 bytes with 4 chars. So, the hash of the first
            // 3 bytes is the beginning of the hash of the first 10 bytes.
            val hash = mangled("validateNames", length = 3).removePrefix("validateNames_")

            hash shouldBe mangled("validateNames").removePrefix("validateNames_").take(4)
        }

        @Test
        fun `rejects a length outside the range from 1 to the digest size`() {
            shouldThrow<IllegalArgumentException> {
                mangled("foo", length = 0)
            }
            shouldThrow<IllegalArgumentException> {
                mangled("foo", length = 33)
            }
        }
    }
}
