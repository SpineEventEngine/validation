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

package io.spine.validation.required

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.option.RequireOption
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.MessageOptionDiscovered
import io.spine.protodata.ast.unpack
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.OPTION_NAME
import io.spine.validation.REQUIRE
import io.spine.validation.RequireMessage
import io.spine.validation.defaultMessage
import io.spine.validation.event.RequireMessageDiscovered
import io.spine.validation.event.requireMessageDiscovered

/**
 * Controls whether a message should be validated with the `(require)` option.
 *
 * Whenever a message marked with `(require)` option is discovered, emits
 * [RequireMessageDiscovered] event if the specified field groups are valid.
 * Please take a look on docs to [ParseFieldGroups] to see how they are validated.
 */
internal class RequirePolicy : Policy<MessageOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = REQUIRE)
        event: MessageOptionDiscovered,
    ): Just<RequireMessageDiscovered> {
        val messageType = event.subject
        val option = event.option.unpack<RequireOption>()
        val groups = ParseFieldGroups(option, messageType, event.file).result
        val message = option.errorMsg.ifEmpty { option.descriptorForType.defaultMessage }
        return requireMessageDiscovered {
            id = messageType.name
            errorMessage = message
            groupsDefinition = option.fields
            group += groups
        }.just()
    }
}

/**
 * A view of a message that is marked with the `(require)` option.
 */
internal class RequireMessageView : View<TypeName, RequireMessage, RequireMessage.Builder>() {

    @Subscribe
    fun on(e: RequireMessageDiscovered) = alter {
        id = e.id
        errorMessage = e.errorMessage
        groupsDefinition = e.groupsDefinition
        addAllGroup(e.groupList)
    }
}
