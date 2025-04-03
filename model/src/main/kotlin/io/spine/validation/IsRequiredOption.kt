/*
` * Copyright 2024, TeamDev. All rights reserved.
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
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.protodata.ast.boolValue
import io.spine.protodata.ast.event.OneofOptionDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.IsRequiredOneofDiscovered
import io.spine.validation.event.isRequiredOneofDiscovered

/**
 * Controls whether a `oneof` group should be validated as `(is_required)`.
 *
 * Whenever a `oneof` groupd marked with `(is_required)` option is discovered,
 * emits [IsRequiredOneofDiscovered] event if the option value is `true`.
 * Otherwise, the policy emits [NoReaction].
 *
 * Note that unlike the `(required)` constraint, this option supports any field type.
 * Protobuf encodes a non-set value as a special case, allowing for checking whether
 * the `oneof` group value is set without relying on default values of field types.
 */
internal class IsRequiredPolicy : Policy<OneofOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = IS_REQUIRED)
        event: OneofOptionDiscovered
    ): EitherOf2<IsRequiredOneofDiscovered, NoReaction> {
        if (!event.option.boolValue) {
            return ignore()
        }
        val oneof = event.group
        return isRequiredOneofDiscovered {
            id = oneofRef {
                type = event.type
                name = oneof
            }
            this.oneOf = oneof
            declaringType = event.type
            errorMessage = "One of the fields in the `${oneof.value}` group must be set."
        }.asA()
    }
}

/**
 * A view of a `oneof` group that is marked with `(is_required) = true` option.
 */
internal class IsRequiredOneofView : View<OneofRef, IsRequiredOneof, IsRequiredOneof.Builder>() {

    @Subscribe
    fun on(e: IsRequiredOneofDiscovered) = alter {
        oneOf = e.oneOf
        declaringType = e.declaringType
        errorMessage = e.errorMessage
    }
}

// TODO:2025-04-03:yevhenii.nadtochii: Have the default message in `options.proto`.

// TODO:2025-04-03:yevhenii.nadtochii: Do we allow a custom error message for this option?

// TODO:2025-04-03:yevhenii.nadtochii: Make content of `OneofOptionDiscovered`
//  similar to `FieldOptionDiscovered`.

// TODO:2025-04-03:yevhenii.nadtochii: Have `OneofRef` and `OneofGroup.ref: OneofRef`
//  and `OneofGroup subject = 7;` in ProtoData.

// TODO:2025-04-03:yevhenii.nadtochii: Clean up the package as Rule stuff is removed.
