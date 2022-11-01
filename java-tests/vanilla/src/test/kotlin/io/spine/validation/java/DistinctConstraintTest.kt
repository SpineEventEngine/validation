/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.spine.validation.java

import com.google.common.truth.Truth8.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth
import io.spine.base.fieldPath
import io.spine.protobuf.TypeConverter.toAny
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidationError
import io.spine.validate.constraintViolation
import io.spine.validation.java.given.ProtoSet
import java.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(distinct)` option should be compiled, so that")
internal class DistinctConstraintTest {

    @Test
    fun `duplicates result is a violation`() {
        val same = "123"
        val msg = protoSetPartial(same, "321", same)
        val error: Optional<ValidationError> = msg.validate()
        assertThat(error)
            .isPresent()
        val violations = error.get().getConstraintViolationList()
        ProtoTruth.assertThat(violations)
            .comparingExpectedFieldsOnly()
            .containsExactly( constraintViolation {
                fieldPath { fieldName.add("element") }
            })
    }

    @Test
    fun `unique elements do not result in a violation`() {
        val msg = protoSet("42", 42, 42.0f, 42.0)
        assertThat(msg.validate()).isEmpty()
    }

    @Test
    fun `empty list does not result in a violation`() {
        val msg = protoSet()
        assertThat(msg.validate())
            .isEmpty()
    }
}

private fun protoSet(): @Validated ProtoSet = ProtoSet.newBuilder().build()

private fun <T> protoSet(vararg element: T): @Validated ProtoSet {
    val result = ProtoSet.newBuilder()
    element.forEach { result.addElement(toAny(it)) }
    return result.build()
}

private fun <T> protoSetPartial(vararg element: T): @NonValidated ProtoSet {
    val result = ProtoSet.newBuilder()
    element.forEach { result.addElement(toAny(it)) }
    return result.buildPartial()
}
