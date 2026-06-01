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
import com.google.protobuf.Descriptors
import com.google.protobuf.Empty
import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.google.protobuf.UnknownFieldSet
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.type.TypeName
import java.util.Optional
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ValidatableMessage` should")
internal class ValidatableMessageSpec {

    @Test
    fun `provide a default 'validate' method`() {
        val message = StubValidatableMessage()
        message.validate().shouldBeEmpty()

        message.capturedPath shouldBe FieldPath.getDefaultInstance()
        message.capturedName shouldBe null
    }
}

/**
 * A stub implementation of [ValidatableMessage] for testing the default [validate] method.
 */
private class StubValidatableMessage : AbstractMessage(), ValidatableMessage {

    var capturedPath: FieldPath? = null
    var capturedName: TypeName? = null

    override fun validate(parentPath: FieldPath, parentName: TypeName?): Optional<ValidationError> {
        capturedPath = parentPath
        capturedName = parentName
        return Optional.empty()
    }

    override fun getDefaultInstanceForType(): Message = this
    override fun getDescriptorForType(): Descriptors.Descriptor = Empty.getDescriptor()
    override fun getAllFields(): Map<Descriptors.FieldDescriptor, Any> = emptyMap()
    override fun hasField(field: Descriptors.FieldDescriptor?): Boolean = false
    override fun getField(field: Descriptors.FieldDescriptor?): Any = Any()
    override fun getRepeatedFieldCount(field: Descriptors.FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: Descriptors.FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
    override fun getParserForType(): Parser<out Message> = throw UnsupportedOperationException()
    override fun newBuilderForType(): Message.Builder = throw UnsupportedOperationException()
    override fun toBuilder(): Message.Builder = throw UnsupportedOperationException()
}
