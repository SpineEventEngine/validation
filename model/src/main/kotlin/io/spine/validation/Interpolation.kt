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

public abstract class Interpolation {

    public fun inTemplate(template: StringExpression): In =
        In(template, this)

    protected abstract fun replace(
        template: StringExpression,
        placeholder: StringExpression,
        replacement: StringExpression
    ): StringExpression

    internal fun doReplace(
        template: StringExpression,
        placeholder: StringExpression,
        replacement: StringExpression
    ) = replace(template, placeholder, replacement)
}

public class In
internal constructor(
    private val template: StringExpression,
    private val interpolation: Interpolation
) {

    public infix fun replace(placeholder: StringExpression): Replace =
        Replace(template, placeholder, interpolation)

    public infix fun replace(replacement: String): Replace =
        replace(StringExpression(replacement))
}

public class Replace
internal constructor(
    private val template: StringExpression,
    private val placeholder: StringExpression,
    private val interpolation: Interpolation
) {

    public infix fun with(replacement: StringExpression): StringExpression =
        interpolation.doReplace(template, placeholder, replacement)

    public infix fun with(replacement: String): StringExpression =
        with(StringExpression(replacement))
}

public class StringExpression(internal val value: String) {

    public companion object {

        @JvmStatic
        public fun inQuotes(value: String): StringExpression =
            StringExpression("\"$value\"")
    }

    override fun toString(): String = value
}
