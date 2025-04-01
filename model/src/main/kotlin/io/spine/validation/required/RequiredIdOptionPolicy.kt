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

package io.spine.validation.required

import io.spine.core.External
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.ast.firstField
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import io.spine.validation.required.event.RequiredFieldDiscovered

/**
 * A policy that marks ID fields in entity state messages as required.
 *
 * The entity state messages are discovered via
 * the [options][io.spine.validation.MessageMarkers.getEntityOptionNameList],
 * specified in [ValidationConfig][io.spine.validation.ValidationConfig].
 *
 * @see RequiredIdPatternPolicy
 */
internal class RequiredIdOptionPolicy : RequiredIdPolicy() {

    private val options: Set<String> by lazy {
        if (config == null) {
            emptySet()
        } else {
            config!!.messageMarkers
                .entityOptionNameList
                .toSet()
        }
    }

    @React
    @Suppress("ReturnCount") // Prefer sooner exit and precise conditions.
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        if (options.isEmpty()) {
            return ignore()
        }
        val type = event.type
        if (!type.isEntityState()) {
            return ignore()
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
