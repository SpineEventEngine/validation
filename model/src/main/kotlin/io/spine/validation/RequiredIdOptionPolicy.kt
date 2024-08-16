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

package io.spine.validation

import io.spine.core.External
import io.spine.protodata.MessageType
import io.spine.protodata.firstField
import io.spine.protodata.event.TypeDiscovered
import io.spine.server.event.React
import io.spine.server.model.NoReaction
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.RuleAdded

/**
 * A policy that marks ID fields in entity state messages as required.
 *
 * The entity state messages are discovered via the options, specified in [ValidationConfig].
 * If ProtoData runs with no config, this policy never produces any validation rules.
 *
 * @see RequiredIdPatternPolicy
 */
internal class RequiredIdOptionPolicy : RequiredIdPolicy() {

    private val options: Set<String> by lazy {
        if (config == null) {
            emptySet()
        } else {
            config!!.messageMarkers.entityOptionNameList.toSet()
        }
    }

    @React
    @Suppress("ReturnCount") // prefer sooner exit and precise conditions.
    override fun whenever(@External event: TypeDiscovered): EitherOf2<RuleAdded, NoReaction> {
        if (options.isEmpty()) {
            return noReaction()
        }
        val type = event.type
        if (!type.isEntityState()) {
            return noReaction()
        }
        val field = type.firstField
        return withField(field)
    }

    private fun MessageType.isEntityState(): Boolean {
        val typeOptions = optionList.map { it.name }
        val result = typeOptions.any { it in options }
        return result
    }
}
