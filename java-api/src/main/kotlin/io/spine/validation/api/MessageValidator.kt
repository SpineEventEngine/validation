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

package io.spine.validation.api

import com.google.protobuf.Message
import io.spine.annotation.SPI

/**
 * A custom validator for Protobuf messages of type [M] that are defined externally.
 *
 * External messages are those that come from dependencies.
 * Protobuf [well-known](https://protobuf.dev/reference/protobuf/google.protobuf/)
 * messages should also be considered external. For such messages, it is impossible
 * to declare validation constraints using the validation options because there's
 * no access to their proto definitions.
 *
 * To be able to validate external messages, one must implement this interface
 * and annotate the implementing class with the [Validator] annotation, specifying
 * the type of the message to validate.
 *
 * An example of the validator declaration for the `Earphones` message:
 *
 * ```kotlin
 * @Validator(Earphones::class)
 * public class EarphonesValidator : MessageValidator<Earphones> {
 *     public override fun validate(message: Earphones): List<DetectedViolation> {
 *         return emptyList() // Always valid.
 *     }
 * }
 * ```
 *
 * ## Applicability
 *
 * A validator is applied only to the local messages of the module it is declared in.
 * For each local message that has a field of type [M], the validation library
 * will invoke a validator when checking that message. Repeated and map fields are supported.
 *
 * Standalone instances of [M] and fields of [M] type that occur in other external
 * messages will not be checked by the validator.
 *
 * Consider the following example:
 *
 * ```proto
 * import "earphones.proto"; // Brings the `Earphones` message from dependencies.
 *
 * // A locally-declared message.
 * message WorkingSetup {
 *
 *     // The field of external message type.
 *     Earphones earphones = 1;
 * }
 * ```
 *
 * If `WorkingSetup` and `EarphonesValidator` are declared within the same module,
 * every instance of `WorkingSetup.earphones` will be checked with the validator.
 *
 * The following cases are not covered:
 *
 * - Standalone instantiations of `Earphones` will not be checked.
 * - Other messages from `earphones.proto` that use `Earphones` as a field will
 *   also not be checked.
 *
 * ## Implementation
 *
 * The message validator does not have restrictions upon how exactly the message
 * must be validated. It can validate particular field or several fields,
 * the whole message instance (fields relations) and even perform a deep validation.
 *
 * It is a responsibility of the validator to provide the correct instances
 * of [DetectedViolation]. Before reporting to the user, the validation library
 * converts [DetectedViolation] to [ConstraintViolation][io.spine.validate.ConstraintViolation].
 * Returning of an empty list means that the message is valid.
 *
 * For each validation, a new instance of [MessageValidator] is created.
 * For that, every implementation of [MessageValidator] must have a public,
 * no-args constructor.
 *
 * ## Restrictions
 *
 * The following restrictions apply:
 *
 * 1) It is prohibited to declare a message validator for the locally compiled message.
 * 2) Having several validators for the same message type is prohibited. One message
 *    can have only one validator.
 *
 * @param M the type of Protobuf [Message] being validated.
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
