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

package io.spine.validation.java.expression

import io.spine.protodata.ast.Field
import io.spine.protodata.java.Expression
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.field
import io.spine.validation.UnsetValue
import io.spine.validation.java.generate.ValidationCodeInjector.MessageScope.message

/**
 * A trait that provides functionality to generate expressions to check
 * if a given [Field] holds its default (unset) value.
 *
 * This trait takes a Protobuf instance of a default value for the field using
 * [UnsetValue] utility, and then converts it to an expression using [JavaValueConverter].
 */
internal interface DefaultValueExpression {

    /**
     * Converts Protobuf values to Java expressions.
     */
    val converter: JavaValueConverter

    /**
     * Returns an expression that checks if this [Field] has the default value.
     */
    fun Field.hasDefaultValue(): Expression<Boolean> {
        val getter = message.field(this).getter<List<*>>()
        return Expression("$getter.equals(${defaultValue()})")
    }

    /**
     * Returns an expression that yields a default value for the given field.
     */
    private fun Field.defaultValue(): Expression<*> {
        val unsetValue = UnsetValue.forField(this)!!
        val expression = converter.valueToCode(unsetValue)
        return expression
    }
}
