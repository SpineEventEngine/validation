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
import io.spine.core.Subscribe
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.validation.event.CompositeRuleAdded
import io.spine.validation.event.MessageWideRuleAdded
import io.spine.validation.event.SimpleRuleAdded

/**
 * A view of messages compiled by the validation library.
 *
 * This view provides information about the compiled messages and their
 * validation constraints, if any.
 *
 * To add one or more validation constraint to the message, emit [SimpleRuleAdded]
 * or [CompositeRuleAdded] events.
 */
internal class CompiledMessageView :
    View<TypeName, CompiledMessage, CompiledMessage.Builder>() {

    @Subscribe
    fun on(@External event: TypeDiscovered) = alter {
        val messageType = event.type
        type = messageType
        name = messageType.name
    }

    @Subscribe
    fun on(event: SimpleRuleAdded) = alter {
        addRule(event.rule.wrap())
    }

    @Subscribe
    fun on(event: CompositeRuleAdded) = alter {
        addRule(event.rule.wrap())
    }

    @Subscribe
    fun on(event: MessageWideRuleAdded) = alter {
        addRule(event.rule.wrap())
    }
}
