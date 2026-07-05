/*
 * Copyright 2026, TeamDev. All rights reserved.
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

import com.google.protobuf.AbstractMessage
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Empty
import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.google.protobuf.UnknownFieldSet
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.string.templateString
import io.spine.type.TypeName
import java.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName("`ValidatingBuilder` should")
internal class ValidatingBuilderSpec {

    private val violation = constraintViolation {
        message = templateString { withPlaceholders = "The field is required." }
    }

    @Test
    fun `return no violations when the builder content is valid`() {
        val builder = StubBuilder(StubMessage(emptyList()))

        builder.validate().shouldBeEmpty()
    }

    @Test
    fun `return violations of invalid content without throwing`() {
        val builder = StubBuilder(StubMessage(listOf(violation)))

        val violations = assertDoesNotThrow { builder.validate() }

        violations shouldBe listOf(violation)
    }

    @Test
    fun `leave the builder content intact`() {
        val builder = StubBuilder(StubMessage(listOf(violation)))

        builder.validate()

        builder.mutations shouldBe 0
    }

    @Test
    fun `treat a message not supporting validation as valid`() {
        val builder = StubBuilder(Empty.getDefaultInstance())

        builder.validate().shouldBeEmpty()
    }
}

/**
 * A stub implementation of [ValidatableMessage] reporting the given [violations]
 * from its [validate] method.
 */
private class StubMessage(
    private val violations: List<ConstraintViolation>
) : AbstractMessage(), ValidatableMessage {

    override fun validate(parentPath: FieldPath, parentName: TypeName?): Optional<ValidationError> =
        if (violations.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(validationError {
                constraintViolation.addAll(violations)
            })
        }

    override fun getDefaultInstanceForType(): Message = this
    override fun getDescriptorForType(): Descriptor = Empty.getDescriptor()
    override fun getAllFields(): Map<FieldDescriptor, Any> = emptyMap()
    override fun hasField(field: FieldDescriptor?): Boolean = false
    override fun getField(field: FieldDescriptor?): Any = Any()
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
    override fun getParserForType(): Parser<out Message> = throw UnsupportedOperationException()
    override fun newBuilderForType(): Message.Builder = throw UnsupportedOperationException()
    override fun toBuilder(): Message.Builder = throw UnsupportedOperationException()
}

/**
 * A stub implementation of [ValidatingBuilder] which "builds" the given [message]
 * and counts invocations of its mutating methods.
 */
private class StubBuilder<M : Message>(
    private val message: M
) : AbstractMessage.Builder<StubBuilder<M>>(), ValidatingBuilder<M> {

    /**
     * The number of times the mutating methods of this builder were called.
     */
    var mutations: Int = 0
        private set

    override fun build(): M = message
    override fun buildPartial(): M = message
    override fun clone(): StubBuilder<M> = StubBuilder(message)
    override fun isInitialized(): Boolean = true
    override fun getDescriptorForType(): Descriptor = message.descriptorForType

    override fun newBuilderForField(field: FieldDescriptor): Message.Builder =
        StubBuilder(message)

    override fun setField(field: FieldDescriptor, value: Any): StubBuilder<M> {
        mutations++
        return this
    }

    override fun clearField(field: FieldDescriptor): StubBuilder<M> {
        mutations++
        return this
    }

    override fun setRepeatedField(
        field: FieldDescriptor,
        index: Int,
        value: Any
    ): StubBuilder<M> {
        mutations++
        return this
    }

    override fun addRepeatedField(field: FieldDescriptor, value: Any): StubBuilder<M> {
        mutations++
        return this
    }

    override fun setUnknownFields(unknownFields: UnknownFieldSet): StubBuilder<M> {
        mutations++
        return this
    }

    override fun getDefaultInstanceForType(): Message = message.defaultInstanceForType
    override fun getAllFields(): Map<FieldDescriptor, Any> = emptyMap()
    override fun hasField(field: FieldDescriptor?): Boolean = false
    override fun getField(field: FieldDescriptor?): Any = Any()
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
}
