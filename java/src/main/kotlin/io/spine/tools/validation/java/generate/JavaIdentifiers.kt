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

import java.security.MessageDigest
import java.util.Base64

/**
 * Returns a Java identifier with an appended hash in the format `<javaIdentifier>_<hash>`.
 *
 * The hash is the URL-safe Base64 encoding of the first [length] bytes of
 * the SHA-256 digest of the [javaIdentifier], filtered to include only valid
 * Java identifier chars. Appending this hash ensures that the generated field
 * or method name does not conflict with existing declarations in the target class.
 *
 * For example, `mangled("fooPattern")` returns `fooPattern_JfU6lQD8acWPQ`.
 *
 * The same identifier always yields the same name, so the generated code
 * stays the same from build to build. Since equal identifiers yield equal names,
 * the identifiers passed to this function must be unique within the target class.
 *
 * @param javaIdentifier The identifier to append the hash to.
 * @param length The number of digest bytes to encode, from 1 to 32.
 * @throws IllegalArgumentException if the [length] is not in the range from 1 to 32.
 */
public fun mangled(javaIdentifier: String, length: Int = HASH_LENGTH): String {
    require(length in 1..DIGEST_SIZE) {
        "The hash length must be in the range [1, $DIGEST_SIZE], but was $length."
    }
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(javaIdentifier.toByteArray())
    val hash = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(digest.copyOf(length))
        .filter(Char::isJavaIdentifierPart)
    return "${javaIdentifier}_$hash"
}

/**
 * The default number of digest bytes encoded in the hash.
 */
private const val HASH_LENGTH = 10

/**
 * The number of bytes in a SHA-256 digest.
 */
private const val DIGEST_SIZE = 32
