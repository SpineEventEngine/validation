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
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.google.protobuf.StringValue
import com.google.protobuf.UnknownFieldSet
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`FieldAwareMessage` should")
internal class FieldAwareMessageSpec {

    private val field = StringValue.getDescriptor().findFieldByName("value")

    @Test
    fun `read field values through the default protobuf API`() {
        val message = DefaultFieldAwareMessage("stored")

        message.readValue(field) shouldBe "stored"
    }

    @Test
    fun `confirm that all field values are reachable`() {
        val message = StubFieldAwareMessage("stored")

        message.checkFieldsReachable() shouldBe true
    }

    @Test
    fun `reject mismatching field-aware reads`() {
        val message = StubFieldAwareMessage("stored", readValue = "different")

        val thrown = assertThrows<IllegalArgumentException> {
            message.checkFieldsReachable()
        }

        thrown.message shouldContain "`readValue(field)` is implemented incorrectly"
    }
}

private class DefaultFieldAwareMessage(
    private val fieldValue: String
) : AbstractMessage(), FieldAwareMessage {

    override fun getDefaultInstanceForType(): Message = this
    override fun getDescriptorForType(): Descriptor = StringValue.getDescriptor()
    override fun getAllFields(): Map<FieldDescriptor, Any> =
        mapOf(valueField to fieldValue)
    override fun hasField(field: FieldDescriptor?): Boolean = field == valueField
    override fun getField(field: FieldDescriptor?): Any = fieldValue
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
    override fun getParserForType(): Parser<out Message> = throw UnsupportedOperationException()
    override fun newBuilderForType(): Message.Builder = throw UnsupportedOperationException()
    override fun toBuilder(): Message.Builder = throw UnsupportedOperationException()

    private companion object {
        val valueField: FieldDescriptor =
            StringValue.getDescriptor().findFieldByName("value")
    }
}

private class StubFieldAwareMessage(
    private val fieldValue: String,
    private val readValue: String = fieldValue
) : AbstractMessage(), FieldAwareMessage {

    override fun readValue(field: FieldDescriptor): Any = readValue
    override fun getDefaultInstanceForType(): Message = this
    override fun getDescriptorForType(): Descriptor = StringValue.getDescriptor()
    override fun getAllFields(): Map<FieldDescriptor, Any> =
        mapOf(valueField to fieldValue)
    override fun hasField(field: FieldDescriptor?): Boolean = field == valueField
    override fun getField(field: FieldDescriptor?): Any = fieldValue
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
    override fun getParserForType(): Parser<out Message> = throw UnsupportedOperationException()
    override fun newBuilderForType(): Message.Builder = throw UnsupportedOperationException()
    override fun toBuilder(): Message.Builder = throw UnsupportedOperationException()

    private companion object {
        val valueField: FieldDescriptor =
            StringValue.getDescriptor().findFieldByName("value")
    }
}
