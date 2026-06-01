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
import io.spine.base.FieldPath
import io.spine.type.TypeName
import java.util.Optional
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("message validation extensions should")
internal class MessageExtensionsSpec {

    @Test
    fun `delegate 'checkValid' to 'Validate_check'`() {
        val message = CopyableValidatableMessage("valid")

        message.checkValid() shouldBe message
    }

    @Test
    fun `copy validatable messages through their validating builder`() {
        val message = CopyableValidatableMessage("initial")

        val copied = message.copy<CopyableValidatableMessage, CopyableValidatingBuilder> {
            value = "changed"
        }

        copied.value shouldBe "changed"
    }

    @Test
    @Suppress("DEPRECATION")
    fun `delegate deprecated 'vBuild' to 'build'`() {
        val builder = CopyableValidatingBuilder("built")

        builder.vBuild().value shouldBe "built"
    }
}

private class CopyableValidatableMessage(
    val value: String
) : AbstractMessage(), ValidatableMessage {

    override fun validate(parentPath: FieldPath, parentName: TypeName?): Optional<ValidationError> =
        Optional.empty()

    override fun getDefaultInstanceForType(): Message = CopyableValidatableMessage("")
    override fun getDescriptorForType(): Descriptor = StringValue.getDescriptor()
    override fun getAllFields(): Map<FieldDescriptor, Any> = mapOf(valueField to value)
    override fun hasField(field: FieldDescriptor?): Boolean = field == valueField
    override fun getField(field: FieldDescriptor?): Any = value
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
    override fun getParserForType(): Parser<out Message> = throw UnsupportedOperationException()
    override fun newBuilderForType(): Message.Builder = CopyableValidatingBuilder()
    override fun toBuilder(): Message.Builder = CopyableValidatingBuilder(value)

    companion object {
        val valueField: FieldDescriptor =
            StringValue.getDescriptor().findFieldByName("value")
    }
}

private class CopyableValidatingBuilder(
    var value: String = ""
) : AbstractMessage.Builder<CopyableValidatingBuilder>(),
    ValidatingBuilder<CopyableValidatableMessage> {

    override fun build(): CopyableValidatableMessage = CopyableValidatableMessage(value)
    override fun buildPartial(): CopyableValidatableMessage = CopyableValidatableMessage(value)
    override fun clone(): CopyableValidatingBuilder = CopyableValidatingBuilder(value)
    override fun isInitialized(): Boolean = true
    override fun getDescriptorForType(): Descriptor = StringValue.getDescriptor()

    override fun newBuilderForField(field: FieldDescriptor): Message.Builder =
        CopyableValidatingBuilder()

    override fun setField(
        field: FieldDescriptor,
        value: Any
    ): CopyableValidatingBuilder {
        this.value = value as String
        return this
    }

    override fun clearField(field: FieldDescriptor): CopyableValidatingBuilder {
        value = ""
        return this
    }

    override fun setRepeatedField(
        field: FieldDescriptor,
        index: Int,
        value: Any
    ): CopyableValidatingBuilder = this

    override fun addRepeatedField(
        field: FieldDescriptor,
        value: Any
    ): CopyableValidatingBuilder = this

    override fun setUnknownFields(unknownFields: UnknownFieldSet): CopyableValidatingBuilder = this
    override fun getDefaultInstanceForType(): Message = CopyableValidatableMessage("")
    override fun getAllFields(): Map<FieldDescriptor, Any> =
        mapOf(CopyableValidatableMessage.valueField to value)

    override fun hasField(field: FieldDescriptor?): Boolean =
        field == CopyableValidatableMessage.valueField

    override fun getField(field: FieldDescriptor?): Any = value
    override fun getRepeatedFieldCount(field: FieldDescriptor?): Int = 0
    override fun getRepeatedField(field: FieldDescriptor?, index: Int): Any = Any()
    override fun getUnknownFields(): UnknownFieldSet = UnknownFieldSet.getDefaultInstance()
}
