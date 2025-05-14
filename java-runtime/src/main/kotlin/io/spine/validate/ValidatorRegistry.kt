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

package io.spine.validate

import com.google.protobuf.Message
import io.spine.validation.api.MessageValidator

/**
 * A global registry that keeps track of message validators used
 * for validating Protobuf messages.
 */
public object ValidatorRegistry {

    private val map = mutableMapOf<Class<out Message>, MessageValidator<*>>()

    init {
        loadMessageValidators().also { println("Content of message-validators: $it") }
            .map { it.split(":") }
            .forEach { (validator, message) ->
                val validatorInstance = Class.forName(validator)
                    .getConstructor()
                    .newInstance() as MessageValidator<Message>
                val messageClass = Class.forName(message) as Class<Message>
                register(messageClass, validatorInstance)
            }

    }

    /**
     * Registers a [validator] for the given message [clazz].
     *
     * The method overrides the previously set validator, if any.
     */
    @JvmStatic
    public fun <T : Message> register(clazz: Class<T>, validator: MessageValidator<T>) {
        map[clazz] = validator
    }

    /**
     * Returns a validator for the given message [clazz].
     *
     * @throws IllegalStateException if there is no a validator for the given [clazz].
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST") // Type safety is enforced by `register()` method signature.
    public fun <T : Message> get(clazz: Class<T>): MessageValidator<T> {
        check(contains(clazz))
        return map[clazz] as MessageValidator<T>
    }

    /**
     * Returns a validator for the given message [clazz], if any.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST") // Type safety is enforced by `register()` method signature.
    public fun <T : Message> find(clazz: Class<T>): MessageValidator<T>? =
        map[clazz] as MessageValidator<T>?

    /**
     * Tells whether the registry has a validator for the given message [clazz].
     */
    @JvmStatic
    public fun contains(clazz: Class<out Message>): Boolean = map.containsKey(clazz)

    public fun size(): Int = map.size

    /**
     * Reads a file from META-INF in the classpath and returns its non-blank lines.
     *
     * @param name the resource path under META-INF (e.g. "message-validators")
     */
    private fun loadMessageValidators(name: String = "message-validators"): List<String> {
        val resourcePath = "META-INF/$name"
        val stream = Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream(resourcePath)
            ?: error("File not found: $resourcePath")
        return stream.bufferedReader(Charsets.UTF_8)
            .readLines()
    }

    /**
     * Removes all validators from the registry.
     */
    @JvmStatic
    internal fun clear() =  map.clear()
}
