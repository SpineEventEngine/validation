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

import io.spine.base.EntityState
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.route.EventRouting

/**
 * A repository for a view on a field marked with a boolean validation option.
 *
 * @param V The type of the views managed by the repository.
 * @param S The type of the view entity state.
 */
internal abstract class BoolFieldOptionRepo<
        V : BoolFieldOptionView<S, *>,
        S : EntityState<FieldId>
        > : ViewRepository<FieldId, V, S>() {

    override fun setupEventRouting(routing: EventRouting<FieldId>) {
        super.setupEventRouting(routing)
        routing.unicast<FieldOptionDiscovered> { e, _ ->
            fieldId {
                type = e.subject.declaringType
                name = e.subject.name
            }
        }
    }
}
