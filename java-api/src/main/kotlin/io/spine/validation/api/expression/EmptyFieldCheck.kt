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

package io.spine.validation.api.expression

import io.spine.protodata.ast.Field
import io.spine.protodata.java.Expression
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.field
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.UnsetValue

/**
 * Provides an expression to check if a given [Field] holds
 * the default (unset) value.
 *
 * It takes a Protobuf instance of a default value for the field type
 * with [UnsetValue] utility, and then converts it to a Java expression
 * using [JavaValueConverter].
 */
// TODO:2025-03-12:yevhenii.nadtochii: This trait can go without `JavaValueConverter`.
//  See issue: https://github.com/SpineEventEngine/validation/issues/199
public interface EmptyFieldCheck {

    /**
     * Converts Protobuf values to Java expressions.
     */
    public val converter: JavaValueConverter

    /**
     * Returns an expression that checks if this [Field] has the default (unset) value.
     */
    public fun Field.hasDefaultValue(): Expression<Boolean> {
        val getter = message.field(this).getter<Any>()
        return Expression("$getter.equals(${defaultValue()})")
    }

    /**
     * Returns an expression that checks if this [Field] has a non-default value set.
     */
    public fun Field.hasNonDefaultValue(): Expression<Boolean> =
        Expression("!${hasDefaultValue()}")

    /**
     * Returns an expression that yields a default value for the given field.
     */
    private fun Field.defaultValue(): Expression<*> {
        val unsetValue = UnsetValue.forField(this)!!
        val expression = converter.valueToCode(unsetValue)
        return expression
    }
}
