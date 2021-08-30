/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.core.External
import io.spine.protodata.OneofGroupExited
import io.spine.protodata.plugin.Policy
import io.spine.protodata.select
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.tuple.EitherOf3
import io.spine.validation.LogicalOperator.OR
import io.spine.validation.event.CompositeRuleAdded
import io.spine.validation.event.SimpleRuleAdded

internal class IsRequiredPolicy :
    Policy<OneofGroupExited>() {

    @React
    override fun whenever(@External event: OneofGroupExited):
            EitherOf3<CompositeRuleAdded, SimpleRuleAdded, Nothing> {
        val id = OneofId
            .newBuilder()
            .setFile(event.file)
            .setMessage(event.type)
            .setName(event.group)
            .build()
        val group = select<RequiredOneofGroup>().withId(id)
        if (!group.isPresent) {
            return EitherOf3.withC(nothing())
        }
        val oneof = group.get()
        val rules = oneof
            .fieldList
            .map { RequiredRule.forField(it).rule() }
        if (rules.size == 1) {
            return EitherOf3.withB(SimpleRuleAdded
                .newBuilder()
                .setType(event.type)
                .setRule(rules.first().simple)
                .build())
        }
        val e = CompositeRuleAdded
            .newBuilder()
            .setType(event.type)
            .setRule(composeAllViaOr(rules))
            .build()
        return EitherOf3.withA(e)
    }

}

private fun composeAllViaOr(allRules: List<Rule>): CompositeRule {
    var rules = allRules
    while (rules.size > 1) {
        rules = rules
            .chunked(2)
            .map { it.compose() }
    }
    return rules.first().composite
}

private fun List<Rule>.compose(): Rule = when (size) {
    2 -> CompositeRule
        .newBuilder()
        .setLeft(component1())
        .setOperator(OR)
        .setRight(component2())
        .build()
        .rule()
    1 -> first()
    else -> throw IllegalStateException("Cannot compose rules `$this`.")
}

private fun SimpleRule.rule(): Rule = Rule
    .newBuilder()
    .setSimple(this)
    .build()

private fun CompositeRule.rule(): Rule = Rule
    .newBuilder()
    .setComposite(this)
    .build()
