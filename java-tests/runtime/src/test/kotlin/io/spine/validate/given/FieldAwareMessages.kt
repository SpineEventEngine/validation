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

package io.spine.validate.given

import com.google.protobuf.ByteString
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.google.protobuf.UnknownFieldSet
import io.spine.test.validate.AggregateState
import io.spine.test.validate.AggregateStateOrBuilder
import io.spine.validate.FieldAwareMessage
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Creates stub [AggregateState] instance.
 */
internal fun msg(): AggregateState {
    return AggregateState.newBuilder()
        .setEntityId("entity ID")
        .setAnotherId("another ID")
        .build()
}

/**
 * A wrapper that has errors implementing [.readValue].
 */
internal class BrokenFieldAware(
    delegate: AggregateState
) : AggregateStateDelegate(delegate), FieldAwareMessage {

    override fun readValue(field: Descriptors.FieldDescriptor): Any {
        // Error here. The `field.getIndex()` value isn't taken into account at all.
        return entityId
    }
}

/**
 * A wrapper that properly implements [readValue].
 */
internal class FieldAwareMsg(
    delegate: AggregateState
) : AggregateStateDelegate(delegate), FieldAwareMessage {

    override fun readValue(field: Descriptors.FieldDescriptor): Any {
        val index = field.index
        return when (index) {
            0 -> entityId
            1 -> anotherId
            else -> error(
                "Wrong field index `$index` passed when reading values of `AggregateState`."
            )
        }
    }
}

/**
 * A wrapper of [AggregateState] message, which acts close to a real message by delegating
 * the calls to the wrapped instance, and allows extending itself.
 *
 * Descendants of this class also implement [io.spine.validate.FieldAwareMessage] mixin interface in test scenarios.
 */
internal open class AggregateStateDelegate(
    private val delegate: AggregateState
) : AggregateStateOrBuilder, Message {

    override fun getEntityId(): String {
        return delegate.entityId
    }

    override fun getEntityIdBytes(): ByteString {
        return delegate.entityIdBytes
    }

    override fun getAnotherId(): String {
        return delegate.anotherId
    }

    override fun getAnotherIdBytes(): ByteString {
        return delegate.anotherIdBytes
    }

    override fun getDefaultInstanceForType(): AggregateState {
        return delegate.defaultInstanceForType
    }

    override fun getDescriptorForType(): Descriptor {
        return delegate.descriptorForType
    }

    override fun isInitialized(): Boolean {
        return delegate.isInitialized
    }

    override fun getAllFields(): Map<Descriptors.FieldDescriptor, Any> {
        return delegate.allFields
    }

    override fun hasOneof(oneof: Descriptors.OneofDescriptor): Boolean {
        return delegate.hasOneof(oneof)
    }

    override fun getOneofFieldDescriptor(
        oneof: Descriptors.OneofDescriptor
    ): Descriptors.FieldDescriptor {
        return delegate.getOneofFieldDescriptor(oneof)
    }

    override fun hasField(field: Descriptors.FieldDescriptor): Boolean {
        return delegate.hasField(field)
    }

    override fun getField(field: Descriptors.FieldDescriptor): Any {
        return delegate.getField(field)
    }

    override fun getRepeatedFieldCount(field: Descriptors.FieldDescriptor): Int {
        return delegate.getRepeatedFieldCount(field)
    }

    override fun getRepeatedField(field: Descriptors.FieldDescriptor, index: Int): Any {
        return delegate.getRepeatedField(field, index)
    }

    override fun getUnknownFields(): UnknownFieldSet {
        return delegate.unknownFields
    }

    override fun findInitializationErrors(): List<String> {
        return delegate.findInitializationErrors()
    }

    override fun getInitializationErrorString(): String {
        return delegate.initializationErrorString
    }

    override fun toString(): String {
        return delegate.toString()
    }

    @Throws(IOException::class)
    override fun writeTo(output: CodedOutputStream) {
        // no-op.
    }

    override fun getSerializedSize(): Int {
        return 0
    }

    override fun getParserForType(): Parser<out Message?>? {
        return null
    }

    override fun toByteString(): ByteString? {
        return null
    }

    override fun toByteArray(): ByteArray {
        return ByteArray(0)
    }

    @Throws(IOException::class)
    override fun writeTo(output: OutputStream) {
        // no-op.
    }

    override fun writeDelimitedTo(output: OutputStream) {
        // no-op.
    }

    override fun newBuilderForType(): Message.Builder? {
        return null
    }

    override fun toBuilder(): Message.Builder? {
        return null
    }

    companion object {

        val descriptor: Descriptor = AggregateState.getDescriptor()

        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(data: ByteBuffer?): AggregateState {
            return AggregateState.parseFrom(data)
        }

        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(
            data: ByteBuffer?,
            reg: ExtensionRegistryLite?
        ): AggregateState {
            return AggregateState.parseFrom(data, reg)
        }

        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(data: ByteString?): AggregateState {
            return AggregateState.parseFrom(data)
        }

        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(
            data: ByteString?,
            reg: ExtensionRegistryLite?
        ): AggregateState {
            return AggregateState.parseFrom(data, reg)
        }

        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(data: ByteArray?): AggregateState {
            return AggregateState.parseFrom(data)
        }

        @Throws(InvalidProtocolBufferException::class)
        fun parseFrom(
            data: ByteArray?,
            reg: ExtensionRegistryLite?
        ): AggregateState {
            return AggregateState.parseFrom(data, reg)
        }

        @Throws(IOException::class)
        fun parseFrom(input: InputStream?): AggregateState {
            return AggregateState.parseFrom(input)
        }

        @Throws(IOException::class)
        fun parseFrom(
            input: InputStream?,
            reg: ExtensionRegistryLite?
        ): AggregateState {
            return AggregateState.parseFrom(input, reg)
        }

        @Throws(IOException::class)
        fun parseDelimitedFrom(input: InputStream?): AggregateState {
            return AggregateState.parseDelimitedFrom(input)
        }

        @Throws(IOException::class)
        fun parseDelimitedFrom(
            input: InputStream?,
            reg: ExtensionRegistryLite?
        ): AggregateState {
            return AggregateState.parseDelimitedFrom(input, reg)
        }

        @Throws(IOException::class)
        fun parseFrom(input: CodedInputStream?): AggregateState {
            return AggregateState.parseFrom(input)
        }

        @Throws(IOException::class)
        fun parseFrom(
            input: CodedInputStream?,
            reg: ExtensionRegistryLite?
        ): AggregateState {
            return AggregateState.parseFrom(input, reg)
        }

        fun newBuilder(): AggregateState.Builder {
            return AggregateState.newBuilder()
        }

        fun newBuilder(prototype: AggregateState?): AggregateState.Builder {
            return AggregateState.newBuilder(prototype)
        }

        val defaultInstance: AggregateState
            get() = AggregateState.getDefaultInstance()

        fun parser(): Parser<AggregateState> {
            return AggregateState.parser()
        }
    }
}
