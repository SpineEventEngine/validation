/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import com.google.protobuf.ByteString.copyFromUtf8
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.BytesCompanion
import io.spine.test.tools.validate.EnumCompanion
import io.spine.test.tools.validate.EnumForGoes.EFG_ITEM1
import io.spine.test.tools.validate.MapCompanion
import io.spine.test.tools.validate.MessageCompanion
import io.spine.test.tools.validate.RepeatedCompanion
import io.spine.test.tools.validate.StringCompanion
import kotlin.reflect.KClass
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.provider.Arguments.arguments

/**
 * Provides data for parameterized [GoesOneWayITest][io.spine.test.options.goes.GoesOneWayITest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object GoesOneWayTestEnv {

    private val fieldValues = listOf(
        MessageCompanion::class to Timestamps.now(),
        EnumCompanion::class to EFG_ITEM1.valueDescriptor,
        StringCompanion::class to "some companion text",
        BytesCompanion::class to copyFromUtf8("some companion data"),
        RepeatedCompanion::class to listOf(1L, 2L, 3L),
        MapCompanion::class to mapOf("key" to 32),
    )

    /**
     * Test data for [io.spine.test.options.goes.GoesOneWayITest.throwIfOnlyTargetFieldSet].
     */
    @JvmStatic
    fun onlyTargetFields() = fieldValues.map { (messageClass, fieldValue) ->
        val fieldType = messageClass.typeUnderTest()
        val fieldName = messageClass.fieldName()
        arguments(messageClass.java, named(fieldType, fieldName), fieldValue)
    }

    /**
     * Test data for [io.spine.test.options.goes.GoesOneWayITest.passIfOnlyCompanionFieldSet].
     */
    @JvmStatic
    fun onlyCompanionFields() = fieldValues.map { (messageCLass, companionValue) ->
        val fieldType = messageCLass.typeUnderTest()
        arguments(messageCLass.java, named(fieldType, COMPANION_FIELD), companionValue)
    }

    /**
     * Test data for [io.spine.test.options.goes.GoesOneWayITest.passIfBothTargetAndCompanionFieldsSet].
     */
    @JvmStatic
    fun bothTargetAndCompanionFields() = fieldValues.flatMap { (messageClass, companionValue) ->
        val companionType = messageClass.typeUnderTest()
        fieldValues.map { (fieldClass, fieldValue) ->
            val fieldType = fieldClass.typeUnderTest()
            val fieldName = fieldClass.fieldName()
            arguments(
                messageClass.java,
                named(companionType, COMPANION_FIELD),
                companionValue,
                named(fieldType, fieldName),
                fieldValue
            )
        }
    }
}

/**
 * Extracts name of the field type, which is under the test from this [KClass].
 *
 * This extension relies on naming consistency within `goes_one_way.proto` message stubs.
 * A prefix of the message name shows the tested data type.
 *
 * For example, for `StringCompanion` the method returns `string`.
 */
private fun KClass<out Message>.typeUnderTest() = simpleName!!
    .substringBefore("Companion")
    .lowercase()

/**
 * Returns a simple field name of the target field under the test.
 *
 * This extension relies on naming consistency within `goes_one_way.proto` message stubs.
 * Each target field is named after the following pattern: `{data_type}_field`.
 *
 * For example, for `StringCompanion` the method returns `string_field`.
 */
private fun KClass<out Message>.fieldName() = "${typeUnderTest()}_field"
