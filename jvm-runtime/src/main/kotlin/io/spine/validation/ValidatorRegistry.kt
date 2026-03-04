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

import com.google.common.reflect.TypeToken
import com.google.protobuf.Message
import io.spine.annotation.VisibleForTesting
import java.lang.reflect.ParameterizedType
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import org.checkerframework.checker.signature.qual.FullyQualifiedName

/**
 * A registry for custom validators of Protobuf messages.
 *
 * This registry allows for dynamic registration and retrieval of custom validators
 * implementing the [MessageValidator] interface.
 *
 * It supports several validators per message type and provides an API for validating
 * messages by applying all associated validators.
 *
 * The registry also automatically loads validators from the classpath using
 * the [ServiceLoader] mechanism.
 */
public object ValidatorRegistry {

    /**
     * Maps a fully qualified Kotlin class name of a message to a list of validators.
     */
    private val validators: MutableMap<@FullyQualifiedName String, MutableList<MessageValidator<*>>> =
        ConcurrentHashMap()

    init {
        loadFromServiceLoader()
    }

    /**
     * Loads validators from the classpath using [ServiceLoader].
     */
    private fun loadFromServiceLoader() {
        val loader = ServiceLoader.load(MessageValidator::class.java)
        loader.forEach { validator ->
            @Suppress("UNCHECKED_CAST")
            val casted = validator as MessageValidator<Message>
            val messageType = casted.messageClass()
            add(messageType, casted)
        }
    }

    /**
     * Adds a custom validator for the specific Protobuf message type.
     *
     * @param cls the class of the message to validate.
     * @param validator the validator to add.
     */
    public fun <M : Message> add(cls: KClass<out M>, validator: MessageValidator<M>) {
        val list = validators.computeIfAbsent(cls.qualifiedName!!) { mutableListOf() }
        if (!list.contains(validator)) {
            list.add(validator)
        }
    }

    /**
     * Removes all validators for the given message type.
     *
     * @param cls the class of the message for which to remove validators.
     */
    public fun remove(cls: KClass<out Message>) {
        validators.remove(cls.qualifiedName)
    }

    /**
     * Clears all registered validators.
     */
    public fun clear() {
        validators.clear()
    }

    /**
     * Validates the given [message] by looking up its type in the registry
     * and applying all associated validators.
     *
     * @param message the message to validate.
     * @return the list of detected violations, or an empty list if no violations were found.
     */
    public fun validate(message: Message): List<DetectedViolation> {
        val cls = message::class.qualifiedName!!
        val associatedValidators = validators[cls] ?: return emptyList()
        return associatedValidators.flatMap { validator ->
            @Suppress("UNCHECKED_CAST")
            val casted = validator as MessageValidator<Message>
            casted.validate(message)
        }
    }
}

/**
 * Obtains the type of the message validated by this [MessageValidator].
 */
@VisibleForTesting
internal fun <M : Message> MessageValidator<M>.messageClass(): KClass<M> {
    val typeToken = TypeToken.of(this::class.java)
    val supertype = typeToken.getSupertype(MessageValidator::class.java)
    val messageType = supertype.type.let {
        val parameterized = it as? ParameterizedType
        parameterized?.actualTypeArguments?.get(0)
    }
    @Suppress("UNCHECKED_CAST")
    return (messageType as Class<M>).kotlin
}
