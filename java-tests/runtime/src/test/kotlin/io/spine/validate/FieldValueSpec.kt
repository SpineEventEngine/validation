/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.validate

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
import com.google.protobuf.Syntax
import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.spine.base.Identifier.newUuid
import io.spine.code.proto.FieldContext
import io.spine.test.validate.field.Stub
import kotlin.streams.toList
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`FieldValue` should")
internal class FieldValueSpec {

    @Nested inner class
    convert {

        @Test
        fun `a map to values`() {
            val map = buildMap {
                put(newUuid(), newUuid())
                put(newUuid(), newUuid())
            }
            val fieldValue = FieldValue.of(map, mapContext())
            assertConversion(map.values, fieldValue)
        }

        @Test
        fun `a repeated field`() {
            val repeated = listOf(newUuid(), newUuid())
            val fieldValue = FieldValue.of(repeated, repeatedContext())
            assertConversion(repeated, fieldValue)
        }

        @Test
        fun `a scalar field`() {
            val scalar = newUuid()
            val fieldValue = FieldValue.of(scalar, scalarContext())
            assertConversion(listOf(scalar), fieldValue)
        }
    }

    @Nested inner class
    `determine 'JavaType' for` {

        @Test
        fun `a map`() {
            val mapValue = FieldValue.of(mapOf<String, Timestamp>(), mapContext())
            mapValue.javaType() shouldBe MESSAGE
        }

        @Test
        fun `a repeated`() {
            val repeatedValue = FieldValue.of(listOf<String>(), repeatedContext())
            repeatedValue.javaType() shouldBe STRING
        }
    }

    @Test
    fun `handle 'Enum' value`() {
        val rawValue = Syntax.SYNTAX_PROTO3
        val enumValue = FieldValue.of(rawValue, scalarContext())
        val expectedValues = listOf(rawValue.valueDescriptor)
        assertConversion(expectedValues, enumValue)
    }

    @Nested inner class
    `check if the value is default for a` {

        @Test
        fun `repeated fields`() {
            assertDefault(FieldValue.of(listOf("", "", ""), repeatedContext()))
            assertNotDefault(
                FieldValue.of(
                    listOf("", "abc", ""),
                    repeatedContext()
                )
            )
        }

        @Test
        fun `map fields`() {
            assertDefault(FieldValue.of(mapOf("aaaa" to ""), mapContext()))
            assertNotDefault(
                FieldValue.of(
                    mapOf(
                        "" to "",
                        "aaaa" to "aaa",
                        " " to ""
                    ),
                    mapContext()
                )
            )
        }

        @Test
        fun `string fields`() {
            assertDefault(FieldValue.of("", scalarContext()))
            assertNotDefault(FieldValue.of(" ", scalarContext()))
        }

        private fun assertDefault(value: FieldValue) {
            value.isDefault shouldBe true
        }

        private fun assertNotDefault(value: FieldValue) {
            value.isDefault shouldBe false
        }
    }

    private fun <T> assertConversion(expectedValues: Collection<T?>, fieldValue: FieldValue) {
        fieldValue.values().toList() shouldBe expectedValues
    }
}

private val descriptor = Stub.getDescriptor()

fun mapContext(): FieldContext {
    val mapField = descriptor.findFieldByName("map")
    return FieldContext.create(mapField)
}

fun repeatedContext(): FieldContext {
    val repeatedField = descriptor.findFieldByName("repeated")
    return FieldContext.create(repeatedField)
}

fun scalarContext(): FieldContext {
    val scalarField = descriptor.findFieldByName("scalar")
    return FieldContext.create(scalarField)
}
