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
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.codegen.java.Expression;
import io.spine.protodata.codegen.java.Literal;
import io.spine.protodata.codegen.java.MessageReference;
import io.spine.protodata.codegen.java.MethodCall;
import io.spine.protodata.codegen.java.Poet;
import io.spine.protodata.codegen.java.This;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.SourceFile;
import io.spine.tools.code.CommonLanguages;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidatableMessage;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import io.spine.validation.MessageValidation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Optional;

import static io.spine.protodata.codegen.java.TypedInsertionPoint.CLASS_SCOPE;
import static io.spine.protodata.codegen.java.TypedInsertionPoint.MESSAGE_IMPLEMENTS;
import static io.spine.protodata.renderer.InsertionPointKt.getCodeLine;
import static java.lang.System.lineSeparator;
import static javax.lang.model.element.Modifier.PUBLIC;

@SuppressWarnings("OverlyCoupledClass")
final class ValidationCode {

    private static final Type OPTIONAL_ERROR =
            new TypeToken<Optional<ValidationError>>() {}.getType();
    @SuppressWarnings("DuplicateStringLiteralInspection") // Duplicates in generated code.
    private static final String VALIDATE = "validate";
    private static final String RETURN_LITERAL = "return $L";
    private static final Expression VIOLATIONS = new Literal("violations");

    private final JavaValidationRenderer renderer;
    private final SourceFile sourceFile;
    private final TypeName typeName;
    private final MessageValidation validation;

    ValidationCode(JavaValidationRenderer renderer,
                   SourceFile file,
                   TypeName name,
                   MessageValidation validation) {
        this.renderer = renderer;
        this.sourceFile = file;
        this.typeName = name;
        this.validation = validation;
    }

    void generate() {
        var atMessageImplements = sourceFile.at(MESSAGE_IMPLEMENTS.forType(typeName));
        atMessageImplements.add(new ClassName(ValidatableMessage.class) + ",");
        var code = generateValidationCode(validation);
        var atClassScope = sourceFile.at(CLASS_SCOPE.forType(typeName));
        atClassScope.add(wrapToMethod(code));
        atClassScope.add(code.supportingMembersLines());
        sourceFile.at(new Validate(typeName))
                  .add(validateBeforeBuild());
    }

    private ImmutableList<String> wrapToMethod(ValidationConstraintCode generated) {
        var code = CodeBlock.builder();
        code.addStatement(newAccumulator());
        code.add(generated.codeBlock());
        code.add(extraInsertionPoint(/*validation.getType()
                                               .getName()*/typeName));
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

    private static ImmutableList<String> validateBeforeBuild() {
        var result = new MessageReference("result");
        var code = CodeBlock.builder()
                .addStatement("$T error = $L", OPTIONAL_ERROR,
                              new MethodCall(result, VALIDATE))
                .beginControlFlow("if (error.isPresent())")
                .addStatement("throw new $T(error.get().getConstraintViolationList())",
                              ValidationException.class)
                .endControlFlow()
                .build();
        return Poet.lines(code);
    }

    private static CodeBlock newAccumulator() {
        return CodeBlock.of("$T<$T> $L = new $T<>()",
                            ArrayList.class,
                            ConstraintViolation.class,
                            VIOLATIONS,
                            ArrayList.class);
    }

    private ValidationConstraintCode generateValidationCode(MessageValidation validation) {
        var msg = This.INSTANCE.asMessage();
        var code = CodeBlock.builder();
        var file = validation.getType()
                             .getFile();
        var typeName = validation.getType()
                                 .getName();
        ImmutableList.Builder<CodeBlock> supportingMembers = ImmutableList.builder();
        for (var rule : validation.getRuleList()) {
            var context = new GenerationContext(
                    rule, msg, file, renderer.typeSystem(), typeName, VIOLATIONS, renderer
            );
            var generator = JavaCodeGeneration.generatorFor(context);
            var block = generator.code();
            code.add(block);
            supportingMembers.add(generator.supportingMembers());
        }
        var result = new ValidationConstraintCode(code.build(), supportingMembers.build());
        return result;
    }

    private static CodeBlock extraInsertionPoint(TypeName name) {
        InsertionPoint insertionPoint = new ExtraValidation(name);
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
        var newOptional = new ClassName(Optional.class)
                .call("of", ImmutableList.of(errorBuilder));
        code.addStatement(RETURN_LITERAL, newOptional);
        code.nextControlFlow("else");
        var optionalEmpty = new ClassName(Optional.class)
                .call("empty");
        code.addStatement(RETURN_LITERAL, optionalEmpty);
        code.endControlFlow();
        return code.build();
    }
}