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

package io.spine.validation

import com.google.protobuf.Message
import io.spine.annotation.SPI

/**
 * A validator for a Protobuf message of type [M].
 *
 * ## Local and external messages
 *
 * 1. **Local messages** are the types for which end-users generate and control
 *    the Java/Kotlin classes by compiling the corresponding `.proto` definitions
 *    within their own codebase. For such messages, the Validation library allows
 *    declaring validation constraints and enforces them with the generated code.
 *
 * 2. **External messages** are the types for which end-users do not generate or control
 *    the Java/Kotlin classes. Because the classes are already generated, users cannot modify
 *    the underlying `.proto` definitions and add validation options at compile time.
 *
 * Both local and external messages can be checked using classes implementing this interface.
 *
 * ## Applying `MessageValidator` to local messages
 *
 * If built-in or custom validation options are not sufficient, you can implement
 * this interface to enforce additional validation rules for local messages.
 *
 * The validator will be applied after the validation of the built-in and custom options in
 * the [generated code][ValidatableMessage.validate].
 *
 * ## Validation of external messages
 *
 * The Validation library provides a mechanism that allows validating of
 * the external messages, **which are used for fields within local messages**.
 *
 * For each field of type [M] within any local message, the library will invoke
 * the [MessageValidator.validate] method when validating the local message.
 *
 * The following Protobuf field types are supported:
 *
 * 1. Singular fields of type [M].
 * 2. Repeated field of type [M].
 * 3. Map field with values of type [M].
 *
 * An example of the validator declaration for the `Earphones` message:
 *
 * ```kotlin
 * public class EarphonesValidator : MessageValidator<Earphones> {
 *     public override fun validate(message: Earphones): List<DetectedViolation> {
 *         return emptyList() // Always valid.
 *     }
 * }
 * ```
 *
 * Note that standalone instances of [M] and fields of [M] type that occur in
 * other external messages **will not be validated**.
 *
 * Consider the following example:
 *
 * ```proto
 * // Brings the `Earphones` message from dependencies.
 * // Suppose we don't control the generated code of declarations from this file.
 * import "earphones.proto";
 *
 * // A locally-declared message.
 * message WorkingSetup {
 *
 *     // The field of external message type.
 *     Earphones earphones = 1;
 * }
 * ```
 *
 * Supposing that the Validation library applied to the module where both `WorkingSetup`
 * and `EarphonesValidator` classes are declared, then the generated code of `WorkingSetup`
 * will apply the validator to each instance passed to the `WorkingSetup.earphones` field.
 *
 * ## Multiple validators for the same message type
 *
 * It is possible to declare more than one validator for the same message type.
 * The order in which they are applied to a message type is not guaranteed.
 *
 * ## Implementation
 *
 * The interface offers flexible validation strategies. Implementations can choose to
 * validate a particular field, several fields, the whole message instance (for example,
 * checking the field relations), and perform a deep validation.
 *
 * It is a responsibility of the validator to provide the correct instances of [DetectedViolation].
 * Before reporting to the user, the library converts [DetectedViolation] to
 * a [ConstraintViolation][ConstraintViolation].
 * Returning of an empty list of violations means that the given message is valid.
 *
 * ## Automatic discovery
 *
 * Every implementation of [MessageValidator] must have a public, no-args constructor.
 *
 * To make the validator is used from the generated code, ensure the implementation is
 * discoverable by [ServiceLoader][java.util.ServiceLoader].
 *
 * @param M the type of Protobuf [Message][Message] being validated.
 *
 * @see ValidatorRegistry
 */
@SPI
public interface MessageValidator<M : Message> {

    /**
     * Validates the given [message].
     *
     * @return the detected violations or empty list.
     */
    public fun validate(message: M): List<DetectedViolation>
}
