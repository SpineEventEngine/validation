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

package io.spine.validation.java

import com.google.common.collect.ImmutableSet
import io.spine.protodata.ast.isMap
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.call

/**
 * Generates the code for the [DistinctCollection][io.spine.validation.DistinctCollection] operator.
 *
 * If the generator serves a map, it checks the [values][Map.values] of the map to be distinct.
 */
internal class DistinctGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    /**
     * Creates an expression checking that a repeated field or a map of a proto message
     * has distinct values.
     *
     * If the field is a map, the generated code checks the values of the map to be distinct.
     *
     * The generated code assumes that if the field contains distinct values, the size of
     * the original collection is equal to the size of the `ImmutableSet` created as a copy
     * of the checked collection.
     */
    override fun condition(): Expression<Boolean> {
        val values = fieldValues()
        return equalityOf(
            MethodCall(values, "size"),
            ClassName(ImmutableSet::class)
                .call<ImmutableSet<*>>("copyOf", values)
                .chain("size")
        )
    }

    private fun fieldValues(): Expression<Collection<*>> {
        val fieldIsMap = ctx.simpleRuleField.isMap
        val fieldValue = ctx.fieldOrElement!!
        return if (fieldIsMap) {
            MethodCall(fieldValue, "values")
        } else {
            // `DistinctGenerator` is instantiated only for repeated fields, so the cast is safe.
            // See `io.spine.validation.java.CustomRuleGeneratorsKt.generatorForCustom`.
            @Suppress("UNCHECKED_CAST")
            fieldValue as Expression<Collection<*>>
        }
    }

    private fun equalityOf(left: Expression<Int>, right: Expression<Int>): Expression<Boolean> =
        Expression(left.toCode() + " == " + right.toCode())
}
