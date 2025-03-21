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

package io.spine.validation

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Message
import io.kotest.matchers.string.shouldContain
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.protobuf.descriptor
import io.spine.protodata.protobuf.field
import kotlin.reflect.KClass
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("`GoesPolicy` should reject the option")
internal class GoesPolicySpec : CompilationErrorTest() {

    @MethodSource("io.spine.validation.GoesPolicyTestEnv#messagesWithUnsupportedTarget")
    @ParameterizedTest(name = "when target field type is `{0}`")
    fun whenTargetFieldHasUnsupportedType(message: KClass<out Message>) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("target")
        error.message shouldContain unsupportedFieldType(field)
    }

    @MethodSource("io.spine.validation.GoesPolicyTestEnv#messagesWithUnsupportedCompanion")
    @ParameterizedTest(name = "when companion's field type is `{0}`")
    fun whenCompanionFieldHasUnsupportedType(message: KClass<out Message>) {
        val descriptor = message.descriptor
        val error = assertCompilationFails(descriptor)
        val field = descriptor.field("companion")
        error.message shouldContain unsupportedCompanionType(field)
    }

    @Test
    fun `the specified companion field does not exist`() {
        val message = GoesNonExistingCompanion.getDescriptor()
        val error = assertCompilationFails(message)
        val companion = "name"
        error.message shouldContain nonExistingCompanion(message, companion)
    }

    @Test
    fun `the field specified itself as its companion`() {
        val message = GoesSelfCompanion.getDescriptor()
        val error = assertCompilationFails(message)
        val field = message.field("name")
        error.message shouldContain selfCompanion(field)
    }
}

private fun unsupportedFieldType(field: Field) =
    "The field type `${field.type.name}` of the `${field.qualifiedName}` field" +
            " is not supported by the `($GOES)` option."

private fun unsupportedCompanionType(field: Field) =
    "The field type `${field.type.name}` of the companion `${field.qualifiedName}` field" +
            " is not supported by the `($GOES)` option."

private fun nonExistingCompanion(message: Descriptor, companionName: String) =
    "The message `${message.fullName}` does not have `$companionName` field" +
            " declared as companion of `target` by the `($GOES)` option."

private fun selfCompanion(field: Field) =
    "The `($GOES)` option cannot use the marked field as its own companion." +
            " Self-referencing is prohibited. Please specify another field." +
            " The invalid field: `${field.qualifiedName}`."
