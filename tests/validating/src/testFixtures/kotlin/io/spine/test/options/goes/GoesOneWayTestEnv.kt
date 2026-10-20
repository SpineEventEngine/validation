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
 * Provides data for parameterized [GoesOneWayIgTest][io.spine.test.options.goes.GoesOneWayIgTest].
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
     * Test data for [io.spine.test.options.goes.GoesOneWayIgTest.throwIfOnlyTargetFieldSet].
     */
    @JvmStatic
    fun onlyTargetFields() = fieldValues.map { (messageClass, fieldValue) ->
        val fieldType = messageClass.typeUnderTest()
        val fieldName = messageClass.fieldName()
        arguments(messageClass.java, named(fieldType, fieldName), fieldValue)
    }

    /**
     * Test data for [io.spine.test.options.goes.GoesOneWayIgTest.passIfOnlyCompanionFieldSet].
     */
    @JvmStatic
    fun onlyCompanionFields() = fieldValues.map { (messageCLass, companionValue) ->
        val fieldType = messageCLass.typeUnderTest()
        arguments(messageCLass.java, named(fieldType, COMPANION_FIELD), companionValue)
    }

    /**
     * Test data for [io.spine.test.options.goes.GoesOneWayIgTest.passIfBothTargetAndCompanionFieldsSet].
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
