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

package io.spine.validation.java;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.System.lineSeparator;

/**
 * Code generated for a validation constraint.
 */
final class ValidationConstraintCode {

    private static final Splitter onNewLine = Splitter.on(lineSeparator());

    /**
     * The code which performs validation.
     */
    private final CodeBlock code;

    /**
     * Class-level declarations used in the validation code.
     */
    private final ImmutableList<CodeBlock> supportingMembers;

    ValidationConstraintCode(CodeBlock code, ImmutableList<CodeBlock> members) {
        this.code = checkNotNull(code);
        this.supportingMembers = checkNotNull(members);
    }

    CodeBlock codeBlock() {
        return code;
    }

    /**
     * Obtains class-level declarations used in the validation code as code lines.
     */
    ImmutableList<String> supportingMembersLines() {
        return supportingMembers.stream()
                .flatMap(code -> onNewLine.splitToStream(code.toString()))
                .collect(toImmutableList());
    }
}
