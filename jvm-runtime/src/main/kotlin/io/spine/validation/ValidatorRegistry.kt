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

import com.google.common.collect.Sets.newConcurrentHashSet
import com.google.common.reflect.TypeToken
import com.google.errorprone.annotations.ThreadSafe
import com.google.protobuf.Message
import io.spine.annotation.VisibleForTesting
import io.spine.base.FieldPath
import io.spine.protobuf.TypeConverter
import io.spine.type.TypeName
import java.lang.reflect.ParameterizedType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import org.checkerframework.checker.signature.qual.FullyQualifiedName
import com.google.protobuf.Any as ProtoAny

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
 *
 * @see MessageValidator
 */
@ThreadSafe
public object ValidatorRegistry {

    /**
     * The name of a key in the placeholder entry for a class name of a validator.
     *
     * The placeholder value is automatically populated with the fully qualified class name
     * of the validator during [validation][validate].
     *
     * @see TemplateString
     */
    public const val VALIDATOR_PLACEHOLDER: String = "validator"

    /**
     * Maps a fully qualified Kotlin class name of a message to a list of validators.
     */
    private val validators: MutableMap<@FullyQualifiedName String,
            MutableSet<MessageValidator<*>>> = ConcurrentHashMap()

    init {
        loadFromServiceLoader()
    }

    /**
     * Loads validators from the classpath using [ServiceLoader].
     */
    @VisibleForTesting
    internal fun loadFromServiceLoader() {
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
     * @param cls The class of the message to validate.
     * @param validator The validator to add.
     */
    @JvmStatic
    public fun <M : Message> add(cls: KClass<out M>, validator: MessageValidator<M>) {
        synchronized(this) {
            validators.compute(cls.qualifiedName!!) { _, currentSet ->
                val set = currentSet ?: newConcurrentHashSet()
                set.add(validator)
                set
            }
        }
    }

    /**
     * Adds a custom validator for the specific Protobuf message type.
     *
     * @param cls The class of the message to validate.
     * @param validator The validator to add.
     */
    @JvmStatic
    public fun <M : Message> add(cls: Class<out M>, validator: MessageValidator<M>) {
        add(cls.kotlin, validator)
    }

    /**
     * Removes all validators for the given message type.
     *
     * @param cls The class of the message for which to remove validators.
     */
    @JvmStatic
    public fun remove(cls: KClass<out Message>) {
        synchronized(this) {
            validators.remove(cls.qualifiedName)
        }
    }

    /**
     * Removes all validators for the given message type.
     *
     * @param cls The class of the message for which to remove validators.
     */
    @JvmStatic
    public fun remove(cls: Class<out Message>) {
        remove(cls.kotlin)
    }

    /**
     * Obtains the validators for the given message type.
     *
     * @param cls The class of the message for which to get validators.
     * @return The set of validators for the given message type,
     *   or an empty set if no validators are registered.
     */
    @JvmStatic
    public fun <M : Message> get(cls: KClass<out M>): Set<MessageValidator<M>> {
        val registered = validators[cls.qualifiedName!!] ?: return emptySet()
        @Suppress("UNCHECKED_CAST")
        return Collections.unmodifiableSet(registered as Set<MessageValidator<M>>)
    }

    /**
     * Obtains the validators for the given message type.
     *
     * @param cls The class of the message for which to get validators.
     * @return The set of validators for the given message type,
     *   or an empty set if no validators are registered.
     */
    @JvmStatic
    public fun <M : Message> get(cls: Class<out M>): Set<MessageValidator<M>> {
        return get(cls.kotlin)
    }

    /**
     * Clears all registered validators.
     */
    @JvmStatic
    public fun clear() {
        synchronized(this) {
            validators.clear()
        }
    }

    /**
     * Validates the given [message] by looking up its type in the registry
     * and applying all associated validators.
     *
     * @param message The message to validate.
     * @param parentPath The path to the field where the validation occurred.
     *   If empty, it means that the validation occurred at the top-level.
     * @param parentName The name of the message type where the validation occurred.
     *   If null, it means that the validation occurred at the top-level.
     * @return The list of detected violations, or an empty list if no violations were found.
     */
    @JvmStatic
    public fun validate(
        message: Message,
        parentPath: FieldPath,
        parentName: TypeName?
    ): List<ConstraintViolation> {
        val cls = message::class.qualifiedName!!
        val associatedValidators = validators[cls]?.toSet() ?: return emptyList()
        val violations = mutableListOf<Pair<@FullyQualifiedName String, DetectedViolation>>()

        associatedValidators.forEach { validator ->
            val validatorClass = validator::class.qualifiedName ?: "UnknownValidator"
            @Suppress("UNCHECKED_CAST")
            val casted = validator as MessageValidator<Message>
            for (violation in casted.validate(message)) {
                violations.add(validatorClass to violation)
            }
        }

        val result = violations.map { (k, v) ->
            constraintViolation {
                this.message = v.message
                    .toBuilder()
                    .putPlaceholderValue(VALIDATOR_PLACEHOLDER, k)
                    .build()
                typeName = if (parentName != null)
                    parentName.value
                else
                    TypeName.of(message).value
                fieldPath = if (v.fieldPath != null) {
                    parentPath.toBuilder()
                        .addAllFieldName(v.fieldPath.fieldNameList)
                        .build()
                } else {
                    parentPath
                }
                fieldValue = v.fieldValue?.let { TypeConverter.toAny(it) }
                    ?: ProtoAny.getDefaultInstance()
            }
        }
        return result
    }

    /**
     * Validates the given [message] by applying all associated validators.
     */
    @JvmStatic
    public fun validate(message: Message): List<ConstraintViolation> =
        validate(message, parentPath = FieldPath.getDefaultInstance(), parentName = null)
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
    @Suppress("UNCHECKED_CAST") // The cast is ensured by the type parameter `M`.
    return (messageType as Class<M>).kotlin
}
