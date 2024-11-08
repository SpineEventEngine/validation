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

package io.spine.validation.java

import com.google.common.base.MoreObjects
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.TypeName
import io.spine.server.query.QueryingClient
import io.spine.validation.MessageValidation
import io.spine.validation.messageValidation
import java.util.*

/**
 * Maps a message type map to corresponding validation rules.
 *
 * @param client The client to request all instances of [MessageValidation] known to
 *  Validation backend by the time of the call to the constructor.
 *
 * @see io.spine.validation.MessageValidationView
 * @see io.spine.validation.MessageValidationRepository
 */
internal class Validations(client: QueryingClient<MessageValidation>) {

    private val map: Map<TypeName, MessageValidation> =
        client.all().associateBy { it.type.name }

    /**
     * Obtains validation for the given type.
     *
     * If there are no validation rules specified for this type, the method returns
     * an instance which refers to the type, containing no validation rules.
     */
    operator fun get(type: MessageType): MessageValidation =
        map[type.name] ?: noValidation(type)

    private fun noValidation(type: MessageType): MessageValidation =
        messageValidation {
            name = type.name
            this@messageValidation.type = type
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as Validations
        return map == that.map
    }

    override fun hashCode(): Int {
        return Objects.hash(map)
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("map", map)
            .toString()
    }
}
