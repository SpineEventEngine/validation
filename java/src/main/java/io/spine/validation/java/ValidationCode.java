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
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.codegen.java.Expression;
import io.spine.protodata.codegen.java.Literal;
import io.spine.protodata.codegen.java.MessageReference;
import io.spine.protodata.codegen.java.MethodCall;
import io.spine.protodata.codegen.java.Poet;
import io.spine.protodata.renderer.SourceAtLine;
import io.spine.protodata.renderer.SourceFile;
import io.spine.validate.NonValidated;
import io.spine.validate.ValidatableMessage;
import io.spine.validate.Validated;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import io.spine.validation.MessageValidation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.codegen.java.TypedInsertionPoint.CLASS_SCOPE;
import static io.spine.protodata.codegen.java.TypedInsertionPoint.MESSAGE_IMPLEMENTS;
import static java.lang.System.lineSeparator;

/**
 * Generates validation code for a given message type specified via
 * {@link MessageValidation} instance.
 *
 * <p>Serves as a method object for the {@link JavaValidationRenderer} passed to the constructor.
 */
@SuppressWarnings("OverlyCoupledClass")
final class ValidationCode {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Duplicates in generated code.
    static final String VALIDATE = "validate";
    static final Type OPTIONAL_ERROR =
            new TypeToken<Optional<ValidationError>>() {}.getType();
    static final Expression VIOLATIONS = new Literal("violations");

    private final JavaValidationRenderer renderer;
    private final SourceFile sourceFile;
    private final MessageValidation validation;
    private final TypeName messageType;

    /**
     * Creates a new instance for generating message validation code.
     *
     * @param renderer
     *         the parent renderer
     * @param validation
     *         the validation rule for which to generate the code
     * @param file
     *         the file to be extended with the validation code
     */
    ValidationCode(JavaValidationRenderer renderer,
                   MessageValidation validation,
                   SourceFile file) {
        this.renderer = checkNotNull(renderer);
        this.sourceFile = checkNotNull(file);
        this.validation = checkNotNull(validation);
        this.messageType = validation.getName();
    }

    /**
     * Generates the code in the linked source file.
     */
    void generate() {
        implementValidatableMessage();
        handleConstraints();
        insertBeforeBuild();
        annotateBuildMethod();
        annotateBuildPartialMethod();
    }

    private void implementValidatableMessage() {
        var atMessageImplements =
                sourceFile.at(MESSAGE_IMPLEMENTS.forType(messageType))
                          .withExtraIndentation(1);
        atMessageImplements.add(new ClassName(ValidatableMessage.class) + ",");
    }

    private void handleConstraints() {
        var atClassScope = classScope();
        var constraints = ValidationConstraintsCode.generate(renderer, validation);
        atClassScope.add(validateMethod(constraints.codeBlock()));
        atClassScope.add(constraints.supportingMembersLines());
    }

    private SourceAtLine classScope() {
        return sourceFile.at(CLASS_SCOPE.forType(this.messageType));
    }

    private ImmutableList<String> validateMethod(CodeBlock constraintsCode) {
        var validateMethod = new ValidateMethodCode(messageType, constraintsCode);
        var methodSpec = validateMethod.generate();
        var lines = ImmutableList.<String>builder();
        lines.addAll(io.spine.util.Text.split(methodSpec.toString()))
             .add(lineSeparator());
        return lines.build();
    }

    private void insertBeforeBuild() {
        sourceFile.at(new ValidateBeforeReturn(messageType))
                  .withExtraIndentation(2)
                  .add(validateBeforeBuild());
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

    private void annotateBuildMethod() {
        var buildMethod = new BuildMethodReturnTypeAnnotation(messageType);
        sourceFile.atInline(buildMethod)
                  .add(annotation(Validated.class));
    }

    private void annotateBuildPartialMethod() {
        var buildPartialMethod = new BuildPartialReturnTypeAnnotation(messageType);
        sourceFile.atInline(buildPartialMethod)
                  .add(annotation(NonValidated.class));
    }

    /**
     * Creates a string to be used in the code when using the given annotation class.
     *
     * @implNote Adds space before `@` so that when the type is fully qualified, the
     *         annotation is: 1) visible better 2) two or more annotations are separated.
     */
    private static String annotation(Class<? extends Annotation> annotationClass) {
        return " @" + annotationClass.getName();
    }
}
