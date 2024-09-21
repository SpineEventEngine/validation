/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.BoolValue
import com.google.protobuf.kotlin.unpack
import io.spine.core.External
import io.spine.core.Where
import io.spine.protobuf.pack
import io.spine.protodata.ast.event.OneofOptionDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.tuple.EitherOf2
import io.spine.validate.Diags.IsRequired.errorMessage
import io.spine.validate.Diags.IsRequired.operatorDescription
import io.spine.validation.event.MessageWideRuleAdded
import io.spine.validation.event.messageWideRuleAdded

/**
 * A policy to add a validation rule with the [RequiredOneof] feature whenever
 * a required `oneof` group with the `(is_required)` option is encountered.
 *
 * Unlike the `(required)` constraint, any field types are allowed,
 * since the `oneof` encodes for a non-set value as a special case.
 */
internal class IsRequiredPolicy : Policy<OneofOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = "is_required")
        event: OneofOptionDiscovered
    ): EitherOf2<MessageWideRuleAdded, Nothing> {
        // We have the option defined in the type. But is it set to `true`?
        val option = event.option.value.unpack<BoolValue>()
        if (!option.value) {
            return EitherOf2.withB(nothing())
        }
        val requiredOneof = requiredOneof {
            name = event.group
        }
        val customOp = customOperator {
            description = operatorDescription
            feature = requiredOneof.pack()
        }
        val messageWideRule = messageWideRule {
            errorMessage = errorMessage(event.group.value)
            operator = customOp
        }
        return EitherOf2.withA(messageWideRuleAdded {
            rule = messageWideRule
            type = event.type
        })
    }
}
