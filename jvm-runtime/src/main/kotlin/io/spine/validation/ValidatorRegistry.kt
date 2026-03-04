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

import com.google.protobuf.Message
import io.spine.annotation.Internal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

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
 * Java's [ServiceLoader] mechanism.
 */
public object ValidatorRegistry {

    private val validators: MutableMap<KClass<out Message>, MutableList<MessageValidator<*>>> =
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
            val messageType = casted.messageType()
            add(messageType, casted)
        }
    }

    /**
     * Adds a custom validator for the specific Protobuf message type.
     *
     * @param type the class of the message to validate.
     * @param validator the validator to add.
     */
    public fun <M : Message> add(type: KClass<out M>, validator: MessageValidator<M>) {
        val list = validators.computeIfAbsent(type) { mutableListOf() }
        if (!list.contains(validator)) {
            list.add(validator)
        }
    }

    /**
     * Removes all validators for the given message type.
     *
     * @param type the class of the message for which to remove validators.
     */
    public fun remove(type: KClass<out Message>) {
        validators.remove(type)
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
     * @param message The message to validate.
     * @return the list of detected violations, or an empty list if no violations were found.
     */
    public fun validate(message: Message): List<DetectedViolation> {
        val type = message::class
        val associatedValidators = validators[type] ?: return emptyList()
        return associatedValidators.flatMap { validator ->
            @Suppress("UNCHECKED_CAST")
            val casted = validator as MessageValidator<Message>
            casted.validate(message)
        }
    }
}

/**
 * Obtains the type of the message validated by this [MessageValidator].
 *
 * This internal extension function attempts to find the [Validator] annotation
 * on the class to determine the message type it validates.
 */
private fun MessageValidator<*>.messageType(): KClass<out Message> {
    val annotation = this::class.annotations.find { it is Validator } as? Validator
    if (annotation != null) {
        return annotation.value
    }
    // Fallback or error if @Validator is missing.
    // Since we are using ServiceLoader, the implementations should be annotated
    // or we need another way to know the type.
    // The `MessageValidator` interface itself doesn't have `messageType()` method.
    throw IllegalStateException(
        "The validator class `${this::class.qualifiedName}` is not annotated " +
                "with `@io.spine.validation.Validator`."
    )
}
