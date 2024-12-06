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

package io.spine.test.options.goes

import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.EnumForGoes
import io.spine.test.tools.validate.MutualBytesCompanion
import io.spine.test.tools.validate.MutualEnumCompanion
import io.spine.test.tools.validate.MutualMapCompanion
import io.spine.test.tools.validate.MutualMessageCompanion
import io.spine.test.tools.validate.MutualRepeatedCompanion
import io.spine.test.tools.validate.MutualStringCompanion
import kotlin.reflect.KClass
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parameterized [GoesMutualITest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object TestDataMutual {

    private val fieldValues = listOf(
        MutualMessageCompanion::class to Timestamps.now(),
        MutualEnumCompanion::class to EnumForGoes.EFG_ITEM1.valueDescriptor,
        MutualStringCompanion::class to "some companion text",
        MutualBytesCompanion::class to ByteString.copyFromUtf8("some companion data"),
        MutualRepeatedCompanion::class to listOf(1L, 2L, 3L),
        MutualMapCompanion::class to mapOf("key" to 32),
    )

    /**
     * Test data for [GoesMutualITest.passIfBothMutuallyDependentFieldsNotSet].
     */
    @JvmStatic
    fun messagesWithInterdependentFields() = fieldValues.map { it.first.java }

    /**
     * Test data for [GoesMutualITest.throwIfOneOfMutuallyDependentFieldsNotSet] and
     * [GoesMutualITest.passIfBothMutuallyDependentFieldsSet] tests.
     */
    @JvmStatic
    fun interdependentFields() = fieldValues.flatMap { (messageClass, companionValue) ->
        val companionType = messageClass.typeUnderTest()
        fieldValues.map { (fieldClass, fieldValue) ->
            val companionName = fieldClass.companionName()
            val fieldType = fieldClass.typeUnderTest()
            val fieldName = fieldClass.fieldName()
            arguments(
                messageClass.java,
                named(companionType, companionName),
                companionValue,
                named(fieldType, fieldName),
                fieldValue
            )
        }
    }
}

/**
 * Extracts name of the field type, which is under test from this [KClass].
 *
 * This extension relies on naming consistency within `goes_mutual.proto` message stubs.
 * The message name shows the tested data type.
 *
 * For example, `MutualStringCompanion` becomes just `string`.
 */
private fun KClass<out Message>.typeUnderTest() = simpleName!!
    .substringAfter("Mutual")
    .substringBefore("Companion")
    .lowercase()

/**
 * Returns a simple field name of the target field under the test.
 *
 * This extension relies on naming consistency within `goes_mutual.proto` message stubs.
 * Each target field is named after the following pattern: `{data_type}_field`.
 *
 * For example, `MutualStringCompanion` becomes `string_field`.
 */
private fun KClass<out Message>.fieldName() = "${typeUnderTest()}_field"

/**
 * Returns a simple field name of the companion field under the test.
 *
 * This extension relies on naming consistency within `goes_mutual.proto` message stubs.
 * Each companion field is named after the following pattern: `{data_type}_companion`.
 *
 * For example, `MutualStringCompanion` becomes `string_companion`.
 */
private fun KClass<out Message>.companionName() = "${typeUnderTest()}_companion"
