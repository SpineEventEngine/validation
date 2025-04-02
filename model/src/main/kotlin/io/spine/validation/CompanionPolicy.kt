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

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import io.spine.protodata.Compilation
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.findOption
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.plugin.Policy
import io.spine.server.event.Just
import io.spine.server.event.Just.Companion.noReaction
import io.spine.server.event.NoReaction

/**
 * Reports a compilation error when a companion option is applied
 * without its primary boolean counterpart.
 *
 * Boolean options usually have a companion message option for
 * specifying an error message. This policy ensures that such
 * options are not used independently.
 *
 * @param [primary] The primary boolean option.
 * @param [companion] The dependent companion option.
 */
internal abstract class CompanionPolicy(
    private val primary: GeneratedExtension<*, *>,
    companion: GeneratedExtension<*, *>
) : Policy<FieldOptionDiscovered>() {

    private val companionName = companion.descriptor.name
    private val primaryName = primary.descriptor.name

    @Suppress(
        "FoldInitializerAndIfToElvis", // We would like to have an explicit `if` check here.
        "SameReturnValue" // The policy does not emit domain events.
    )
    protected fun checkWithPrimary(event: FieldOptionDiscovered): Just<NoReaction> {
        if (event.option.name != companionName) {
            return noReaction
        }

        val field = event.subject
        val primaryOption = field.findOption(primary)
        if (primaryOption == null) {
            Compilation.error(event.file, field.span) {
                "The `${field.qualifiedName}` field has the `($companionName)` companion option" +
                        " applied without its primary `($primaryName)` boolean option." +
                        " Companion options must always be used together with their primary" +
                        " boolean counterparts."
            }
        }

        return noReaction
    }
}
