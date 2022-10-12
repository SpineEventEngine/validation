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
import io.spine.core.External
import io.spine.core.Where
import io.spine.protobuf.pack
import io.spine.protodata.OneofOptionDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.MessageWideRuleAdded

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
        @External @Where(field = OPTION_NAME, equals = "is_required") event: OneofOptionDiscovered
    ): EitherOf2<MessageWideRuleAdded, Nothing> {
        // We have the option defined in the type. But is it set to `true`?
        val option = event.option.value.unpack(BoolValue::class.java)
        if (!option.value) {
            return EitherOf2.withB(nothing())
        }

        val feature = RequiredOneof.newBuilder()
            .setName(event.group)
            .build()
        val operator = CustomOperator.newBuilder()
            .setDescription("One of the fields must be set.")
            .setFeature(feature.pack())
            .build()
        val rule = MessageWideRule.newBuilder()
            .setErrorMessage("One of the fields in the `${event.group.value}` group must be set.")
            .setOperator(operator)
            .build()
        return EitherOf2.withA(MessageWideRuleAdded.newBuilder()
            .setRule(rule)
            .setType(event.type)
            .build())
    }
}
