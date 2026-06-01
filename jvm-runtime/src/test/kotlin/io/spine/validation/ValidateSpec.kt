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
import com.google.protobuf.Timestamp
import com.google.protobuf.UnknownFieldSet
import com.google.protobuf.timestamp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.protobuf.AnyPacker
import io.spine.string.templateString
import io.spine.type.TypeName
import java.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.google.protobuf.Any as ProtoAny

@DisplayName("`Validate` should")
internal class ValidateSpec {

    @BeforeEach
    fun setUp() {
        ValidatorRegistry.clear()
    }

    @Test
    fun `return a valid message passed to 'check'`() {
        val message = Timestamp.getDefaultInstance()

        Validate.check(message) shouldBe message
    }

    @Test
    fun `throw if a message passed to 'check' is invalid`() {
        val violation = constraintViolation {
            message = templateString { withPlaceholders = "Invalid" }
        }
        val message = StubValidateMessage(listOf(violation))

        val thrown = assertThrows<ValidationException> {
            Validate.check(message)
        }

        thrown.constraintViolations shouldBe listOf(violation)
    }

    @Test
    fun `unpack known 'Any' messages before validation`() {
        ValidatorRegistry.add(Timestamp::class, TimestampValidator())
        val timestamp = timestamp {
            nanos = -1
        }
        val packed = AnyPacker.pack(timestamp)

        val violations = Validate.violationsOf(packed)

        violations shouldHaveSize 1
        violations.single().fieldPath.fieldNameList shouldBe listOf("nanos")
    }

    @Test
    fun `skip unpacking unknown 'Any' messages`() {
        val packed = ProtoAny.newBuilder()
            .setTypeUrl("type.example/unknown.Message")
            .build()

        Validate.violationsOf(packed).shouldBeEmpty()
    }

    @Test
    fun `use violations returned by a 'ValidatableMessage'`() {
        val violation = constraintViolation {
            message = templateString { withPlaceholders = "From message" }
        }
        val message = StubValidateMessage(listOf(violation))

        Validate.violationsOf(message) shouldBe listOf(violation)
    }
}

private class StubValidateMessage(
    private val violations: List<ConstraintViolation>
) : AbstractMessage(), ValidatableMessage {

    override fun validate(parentPath: FieldPath, parentName: TypeName?): Optional<ValidationError> {
        return if (violations.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(validationError {
                constraintViolation.addAll(violations)
            })
        }
    }

    override fun getDefaultInstanceForType(): Message = this
    override fun getDescriptorForType(): Descriptor = Empty.getDescriptor()
    override fun getAllFields(): Map<FieldDescriptor, kotlin.Any> = emptyMap()
    override fun hasField(field: FieldDescriptor?): Boolean = false
    override fun getField(field: FieldDescriptor?): kotlin.Any = kotlin.Any()
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): kotlin.Any =
        kotlin.Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
    override fun getParserForType(): Parser<out Message> = throw UnsupportedOperationException()
    override fun newBuilderForType(): Message.Builder = throw UnsupportedOperationException()
    override fun toBuilder(): Message.Builder = throw UnsupportedOperationException()
}
