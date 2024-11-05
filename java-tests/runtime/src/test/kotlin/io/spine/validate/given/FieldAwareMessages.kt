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
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Message
import com.google.protobuf.Parser
import io.spine.test.validate.AggregateState
import io.spine.test.validate.AggregateStateOrBuilder
import io.spine.validate.FieldAwareMessage
import java.io.IOException
import java.io.OutputStream

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
 * A wrapper that has errors implementing [readValue].
 */
internal class BrokenFieldAware(
    delegate: AggregateState
) : AggregateStateProxy(delegate), FieldAwareMessage {

    override fun readValue(field: FieldDescriptor): Any {
        // Error here. The `field.getIndex()` value isn not taken into account at all.
        return entityId
    }
}

/**
 * A wrapper that properly implements [readValue].
 */
internal class FieldAwareMsg(
    delegate: AggregateState
) : AggregateStateProxy(delegate), FieldAwareMessage {

    override fun readValue(field: FieldDescriptor): Any =
        when (val index = field.index) {
            0 -> entityId
            1 -> anotherId
            else -> error(
                "Wrong field index `$index` passed when reading values of `AggregateState`."
            )
        }
}

/**
 * A wrapper of [AggregateState] message, which acts close to a real message by delegating
 * the calls to the wrapped instance, and allows extending itself.
 *
 * Descendants of this class also implement [io.spine.validate.FieldAwareMessage] mixin
 * interface in test scenarios.
 */
internal open class AggregateStateProxy(
    private val delegate: AggregateState
) : AggregateStateOrBuilder by delegate, Message {

    /* We cannot use the delegation via `by` for `Message` because we already used it
       for `AggregateStateOrBuilder`. So, we do it directly via the property. */

    @Throws(IOException::class)
    override fun writeTo(output: CodedOutputStream) = delegate.writeTo(output)
    override fun getSerializedSize(): Int = delegate.serializedSize
    override fun getParserForType(): Parser<out Message> = delegate.parserForType
    override fun toByteString(): ByteString = delegate.toByteString()
    override fun toByteArray(): ByteArray = delegate.toByteArray()
    @Throws(IOException::class)
    override fun writeTo(output: OutputStream) = delegate.writeTo(output)
    override fun writeDelimitedTo(output: OutputStream) = delegate.writeDelimitedTo(output)
    override fun newBuilderForType(): Message.Builder = delegate.newBuilderForType()
    override fun toBuilder(): Message.Builder = delegate.toBuilder()
}
