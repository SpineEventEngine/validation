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
 * Provides data for parameterized [io.spine.test.options.goes.GoesMutualIgTest].
 */
@Suppress("unused") // Data provider for parameterized test.
internal object GoesMutualTestEnv {

    private val fieldValues = listOf(
        MutualMessageCompanion::class to Timestamps.now(),
        MutualEnumCompanion::class to EnumForGoes.EFG_ITEM1.valueDescriptor,
        MutualStringCompanion::class to "some companion text",
        MutualBytesCompanion::class to ByteString.copyFromUtf8("some companion data"),
        MutualRepeatedCompanion::class to listOf(1L, 2L, 3L),
        MutualMapCompanion::class to mapOf("key" to 32),
    )

    /**
     * Test data for [io.spine.test.options.goes.GoesMutualIgTest.passIfBothMutuallyDependentFieldsNotSet].
     */
    @JvmStatic
    fun messagesWithInterdependentFields() = fieldValues.map { it.first.java }

    /**
     * Test data for the following tests:
     *
     * 1. [io.spine.test.options.goes.GoesMutualIgTest.throwIfOneOfMutuallyDependentFieldsNotSet].
     * 2. [io.spine.test.options.goes.GoesMutualIgTest.passIfBothMutuallyDependentFieldsSet].
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
