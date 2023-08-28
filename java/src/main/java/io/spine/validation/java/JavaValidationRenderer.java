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
import io.spine.protodata.MessageType;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.codegen.java.JavaRenderer;
import io.spine.protodata.codegen.java.MessageReference;
import io.spine.protodata.codegen.java.MethodCall;
import io.spine.protodata.codegen.java.Poet;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.renderer.SourceFile;
import io.spine.protodata.renderer.SourceFileSet;
import io.spine.protodata.type.TypeSystem;
import io.spine.validate.NonValidated;
import io.spine.validate.Validated;
import io.spine.validate.ValidationException;
import io.spine.validation.MessageValidation;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.codegen.java.Ast2Java.javaFile;
import static io.spine.validation.java.ValidationCode.OPTIONAL_ERROR;
import static io.spine.validation.java.ValidationCode.VALIDATE;
import static java.util.stream.Collectors.toSet;

/**
 * A {@link Renderer} for the validation code in Java.
 *
 * <p>Inserts code into the {@link ValidateBeforeReturn} insertion point.
 *
 * <p>The generated code assumes there is a variable called {@code result}. Its type is the type of
 * the validated message. The variable holds the value of the message to validate.
 *
 * <p>The generated code is a number of code lines. It does not contain declarations (classes,
 * methods, etc.).
 *
 * <p>If the validation rules are broken, throws a {@link io.spine.validate.ValidationException}.
 */
@SuppressWarnings("unused" /* Loaded by ProtoData via reflection. */)
public final class JavaValidationRenderer extends JavaRenderer {

    private @MonotonicNonNull SourceFileSet sources;
    private @MonotonicNonNull TypeSystem typeSystem;
    private @MonotonicNonNull Validations validations;

    @Override
    protected void render(SourceFileSet sources) {
        this.sources = sources;
        this.typeSystem = TypeSystem.from(this);
        this.validations = findValidations();
        var messageTypes = queryMessageTypes();
        messageTypes.forEach(this::generateCode);
        annotateGeneratedMessages(sources, messageTypes);
        plugValidationIntoBuild(sources, messageTypes);
    }

    TypeSystem typeSystem() {
        return checkNotNull(typeSystem);
    }

    private Validations findValidations() {
        var client = select(MessageValidation.class);
        return new Validations(client);
    }

    private Set<MessageWithFile> queryMessageTypes() {
        return select(ProtobufSourceFile.class)
                .all()
                .stream()
                .flatMap(JavaValidationRenderer::messages)
                .collect(toSet());
    }

    private static Stream<MessageWithFile> messages(ProtobufSourceFile file) {
        return file
                .getTypeMap()
                .values()
                .stream()
                .map(m -> MessageWithFile.newBuilder()
                        .setMessage(m)
                        .setDeclaredIn(file.getFile())
                        .build());
    }

    private void generateCode(MessageWithFile type) {
        var message = type.getMessage();
        var javaFile = javaFile(message, type.getDeclaredIn());
        sources.findFile(javaFile).ifPresent(
                sourceFile -> addValidationCode(sourceFile, message)
        );
    }

    private void addValidationCode(SourceFile sourceFile, MessageType type) {
        var validation = validations.get(type);
        var typeName = validation.getName();

        var validationCode = new ValidationCode(this, validation, sourceFile);
        validationCode.generate();
    }

    private static void annotateGeneratedMessages(
            SourceFileSet sources, Set<MessageWithFile> messageTypes
    ) {
        messageTypes.stream()
                .map(m -> javaFile(m.getMessage(), m.getDeclaredIn()))
                .flatMap(path -> sources.findFile(path).stream())
                .distinct()
                .forEach(JavaValidationRenderer::addAnnotations);
    }

    private static void addAnnotations(SourceFile file) {
        annotateBuildMethod(file);
        annotateBuildPartialMethod(file);
    }

    private static void annotateBuildMethod(SourceFile sourceFile) {
        var buildMethod = new BuildMethodReturnTypeAnnotation();
        sourceFile.atInline(buildMethod)
                  .add(annotation(Validated.class));
    }

    private static void annotateBuildPartialMethod(SourceFile sourceFile) {
        var buildPartialMethod = new BuildPartialReturnTypeAnnotation();
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

    private static void plugValidationIntoBuild(
            SourceFileSet sources, Set<MessageWithFile> messageTypes
    ) {
        messageTypes.stream()
                .map(m -> javaFile(m.getMessage(), m.getDeclaredIn()))
                .flatMap(path -> sources.findFile(path).stream())
                .distinct()
                .forEach(JavaValidationRenderer::insertBeforeBuild);
    }

    private static void insertBeforeBuild(SourceFile sourceFile) {
        sourceFile.at(new ValidateBeforeReturn())
                  .withExtraIndentation(2)
                  .add(validateBeforeBuild());
    }

    private static ImmutableList<String> validateBeforeBuild() {
        var result = new MessageReference("result");
        var code = CodeBlock.builder()
                .addStatement("$T error = $L",
                              OPTIONAL_ERROR,
                              new MethodCall(result, VALIDATE))
                .beginControlFlow("if (error.isPresent())")
                .addStatement("throw new $T(error.get().getConstraintViolationList())",
                              ValidationException.class)
                .endControlFlow()
                .build();
        return Poet.lines(code);
    }
}
