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

package io.spine.validation.java

import io.spine.protodata.ast.TypeName
import io.spine.validation.java.protodata.CodeBlock
import io.spine.validation.java.protodata.MemberDeclaration

/**
 * Generates Java code for a specific option.
 */
internal interface OptionGenerator {

    /**
     * Generates validation code for the option within the provided
     * message [type].
     *
     * If multiple fields declare the option, the returned code handles them all.
     */
    fun codeFor(type: TypeName): OptionCode
}

/**
 * Java code handling all applications of a specific option within a message.
 */
internal class OptionCode(

    /**
     * Code blocks to be added to the `validate()` method of the message.
     */
    val constraints: List<CodeBlock>,

    /**
     * Additional class-level declarations required by the validation logic.
     *
     * Some constraints may require defining extra fields or methods.
     */
    val declarations: List<MemberDeclaration> = emptyList()
)
