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
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.findOption
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.check

/**
 * Reports a compilation error if the [companion] option is applied
 * to the given [field] without the [primary] option.
 *
 * Some options have a companion option for specifying an error message.
 * This method ensures that a companion option is not used independently.
 */
internal fun checkBothApplied(
    companion: GeneratedExtension<*, *>,
    primary: GeneratedExtension<*, *>,
    field: Field,
    file: File
) {
    val primaryOption = field.findOption(primary)
    val primaryName = primaryOption?.name
    val companionName = companion.descriptor.name
    Compilation.check(primaryOption != null, file, field.span) {
        "The `${field.qualifiedName}` field has the `($companionName)` companion option" +
                " applied without its primary `($primaryName)` option. Companion options" +
                " must always be used together with their primary counterparts."
    }
}
