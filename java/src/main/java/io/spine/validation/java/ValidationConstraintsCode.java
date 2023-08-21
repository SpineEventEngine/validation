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
import io.spine.protodata.FilePath;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.JavaValueConverter;
import io.spine.protodata.codegen.java.MessageReference;
import io.spine.protodata.codegen.java.MessageTypeConvention;
import io.spine.protodata.codegen.java.This;
import io.spine.validation.MessageValidation;
import io.spine.validation.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.validation.java.ValidationCode.VIOLATIONS;
import static java.lang.System.lineSeparator;

/**
 * Code generated for validations constraints specified in a message type.
 */
final class ValidationConstraintsCode {

    private static final Splitter onNewLine = Splitter.on(lineSeparator());

    /**
     * The parent renderer which this method object serves.
     */
    private final JavaValidationRenderer renderer;

    /**
     * The validation rule for which to generate the code.
     */
    private final MessageValidation validation;

    /**
     * The name of the message type for which to generate the code.
     */
    private final TypeName messageType;

    /**
     * The file which declares the message type.
     */
    private final FilePath declaringFile;

    /**
     * The expression for referencing the message in the code.
     */
    private final MessageReference messageReference = This.INSTANCE.asMessage();

    /**
     * The code which performs validation.
     */
    private final CodeBlock.Builder code;

    /**
     * Class-level declarations used in the validation code.
     */
    private final ImmutableList.Builder<CodeBlock> supportingMembers;

    private ValidationConstraintsCode(JavaValidationRenderer r, MessageValidation v) {
        this.renderer = r;
        this.validation = v;
        this.messageType = v.getName();
        this.declaringFile = validation.getType().getFile();

        this.code = CodeBlock.builder();
        this.supportingMembers = ImmutableList.builder();
    }

    /**
     * Creates a new instance with the generated validation constraints code.
     */
    static ValidationConstraintsCode generate(JavaValidationRenderer r, MessageValidation v) {
        checkNotNull(r);
        checkNotNull(v);
        var result = new ValidationConstraintsCode(r, v);
        result.generate();
        return result;
    }

    /**
     * Obtains the generated code block.
     */
    CodeBlock codeBlock() {
        return code.build();
    }

    /**
     * Obtains class-level declarations used in the validation code as code lines.
     */
    ImmutableList<String> supportingMembersLines() {
        return supportingMembers.build()
                .stream()
                .flatMap(code -> onNewLine.splitToStream(code.toString()))
                .collect(toImmutableList());
    }

    private void generate() {
        for (var rule : validation.getRuleList()) {
            addRule(rule);
        }
    }

    private void addRule(Rule rule) {
        var context = newContext(rule);
        var generator = JavaCodeGeneration.generatorFor(context);
        var block = generator.code();
        code.add(block);
        supportingMembers.add(generator.supportingMembers());
    }

    private GenerationContext newContext(Rule rule) {
        var typeSystem = renderer.typeSystem();
        return new GenerationContext(rule,
                                     messageReference,
                                     declaringFile,
                                     typeSystem,
                                     messageType,
                                     VIOLATIONS,
                                     renderer);
    }
}
