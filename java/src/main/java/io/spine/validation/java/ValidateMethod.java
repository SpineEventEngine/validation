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

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.tools.code.CommonLanguages;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationError;

import java.util.ArrayList;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.renderer.InsertionPointKt.getCodeLine;
import static io.spine.validation.java.ValidationCode.OPTIONAL_ERROR;
import static io.spine.validation.java.ValidationCode.VALIDATE;
import static io.spine.validation.java.ValidationCode.VIOLATIONS;
import static java.lang.System.lineSeparator;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Wraps the passed constraints code into a method.
 */
final class ValidateMethod {

    private static final String RETURN_LITERAL = "return $L";

    private final CodeBlock constraintsCode;
    private final TypeName messageType;

    ValidateMethod(TypeName messageType, CodeBlock constraintsCode) {
        this.constraintsCode = checkNotNull(constraintsCode);
        this.messageType = checkNotNull(messageType);
    }

    ImmutableList<String> generate() {
        var code = CodeBlock.builder();
        code.addStatement(newAccumulator());
        code.add(constraintsCode);
        code.add(extraInsertionPoint());
        code.add(generateValidationError());
        var validate = MethodSpec.methodBuilder(VALIDATE)
                .returns(OPTIONAL_ERROR)
                .addModifiers(PUBLIC)
                .addCode(code.build())
                .build();
        var methodLines = validate.toString()
                                  .split(lineSeparator());
        return ImmutableList.copyOf(methodLines);
    }

    private static CodeBlock newAccumulator() {
        return CodeBlock.of("$T<$T> $L = new $T<>()",
                            ArrayList.class,
                            ConstraintViolation.class,
                            VIOLATIONS,
                            ArrayList.class);
    }

    private CodeBlock extraInsertionPoint() {
        InsertionPoint insertionPoint = new ExtraValidation(messageType);
        var java = CommonLanguages.java();
        var line = java.comment(getCodeLine(insertionPoint)) + lineSeparator();
        return CodeBlock.of(line);
    }

    private static CodeBlock generateValidationError() {
        var code = CodeBlock.builder();
        code.beginControlFlow("if (!$L.isEmpty())", VIOLATIONS);
        var errorBuilder = new ClassName(ValidationError.class).newBuilder()
                .chainAddAll("constraint_violation", VIOLATIONS)
                .chainBuild();
        var optional = new ClassName(Optional.class);
        var optionalOf = optional.call("of", ImmutableList.of(errorBuilder));
        code.addStatement(RETURN_LITERAL, optionalOf);
        code.nextControlFlow("else");
        var optionalEmpty = optional.call("empty");
        code.addStatement(RETURN_LITERAL, optionalEmpty);
        code.endControlFlow();
        return code.build();
    }
}
