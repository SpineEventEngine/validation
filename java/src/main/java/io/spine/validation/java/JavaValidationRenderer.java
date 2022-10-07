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
import io.spine.protodata.File;
import io.spine.protodata.FilePath;
import io.spine.protodata.MessageType;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.codegen.java.Expression;
import io.spine.protodata.codegen.java.JavaRenderer;
import io.spine.protodata.codegen.java.Literal;
import io.spine.protodata.codegen.java.MessageReference;
import io.spine.protodata.codegen.java.MethodCall;
import io.spine.protodata.codegen.java.Poet;
import io.spine.protodata.codegen.java.This;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.renderer.SourceFile;
import io.spine.protodata.renderer.SourceFileSet;
import io.spine.tools.code.CommonLanguages;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidatableMessage;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import io.spine.validation.MessageValidation;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import static io.spine.protodata.codegen.java.Ast2Java.javaFile;
import static io.spine.protodata.codegen.java.TypedInsertionPoint.CLASS_SCOPE;
import static io.spine.protodata.codegen.java.TypedInsertionPoint.MESSAGE_IMPLEMENTS;
import static io.spine.protodata.renderer.InsertionPointKt.getCodeLine;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.System.lineSeparator;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * A {@link Renderer} for the validation code in Java.
 *
 * <p>Inserts code into the {@link Validate} insertion point.
 *
 * <p>The generated code assumes there is a variable called {@code result}. Its type is the type of
 * the validated message. The variable holds the value of the message to validate.
 *
 * <p>The generated code is a number of code lines. It does not contain declarations (classes,
 * methods, etc.).
 *
 * <p>If the validation rules are broken, throws a {@link io.spine.validate.ValidationException}.
 */
@SuppressWarnings({
        "unused",  // Loaded by ProtoData via reflection.
        "OverlyCoupledClass" // Entry point for the validation gen.
})
public final class JavaValidationRenderer extends JavaRenderer {

    private static final Type OPTIONAL_ERROR =
            new TypeToken<Optional<ValidationError>>() {}.getType();

    @SuppressWarnings("DuplicateStringLiteralInspection") // Duplicates in generated code.
    private static final String VALIDATE = "validate";

    private static final String RETURN_LITERAL = "return $L";

    /**
     * Amount of indents to add before the validation code.
     *
     * <p>One unit corresponds to four space characters.
     *
     * <p>We could in theory calculate a more appropriate indentation level for every case, taking
     * into account the level of nesting of a message, etc. It is, however, good enough for now
     * for generated code just to have eight spaces of indentation.
     */
    private static final int INDENT_LEVEL = 2;
    private static final Expression VIOLATIONS = new Literal("violations");

    private @MonotonicNonNull SourceFileSet sources;
    private @MonotonicNonNull TypeSystem typeSystem;
    private @MonotonicNonNull Validations validations;

    @Override
    protected void render(SourceFileSet sources) {
        this.sources = sources;
        this.typeSystem = bakeTypeSystem();
        this.validations = findValidations();
        var messageTypes = queryMessageTypes();
        messageTypes.forEach(this::generateCode);
    }

    private TypeSystem bakeTypeSystem() {
        var files = select(ProtobufSourceFile.class).all();
        var types = TypeSystem.newBuilder();
        for (var file : files) {
            file.getTypeMap().values().forEach(type -> types.put(file.getFile(), type));
            file.getEnumTypeMap().values().forEach(type -> types.put(file.getFile(), type));
        }
        return types.build();
    }

    private Validations findValidations() {
        var client = select(MessageValidation.class);
        return new Validations(client);
    }

    private Stream<MessageType> queryMessageTypes() {
        return select(ProtobufSourceFile.class)
                .all()
                .stream()
                .flatMap(file -> file.getTypeMap().values().stream());
    }

    private void generateCode(MessageType type) {
        var protoFile = findProtoFile(type.getFile());
        var javaFile = javaFile(type, protoFile);
        sources.findFile(javaFile).ifPresent(sourceFile -> {
            addValidationCode(sourceFile, type);
        });
    }

    private File findProtoFile(FilePath path) {
        return select(ProtobufSourceFile.class)
                .withId(path)
                .orElseThrow(() -> newIllegalArgumentException(
                        "No such Protobuf file: `%s`.",
                        path.getValue()
                )).getFile();
    }

    private void addValidationCode(SourceFile sourceFile, MessageType type) {
        var validation = validations.get(type);
        var typeName = validation.getName();
        var atMessageImplements = sourceFile.at(MESSAGE_IMPLEMENTS.forType(typeName));
        atMessageImplements.add(new ClassName(ValidatableMessage.class) + ",");
        var code = generateValidationCode(validation);
        var atClassScope = sourceFile.at(CLASS_SCOPE.forType(typeName));
        atClassScope.add(wrapToMethod(code, validation));
        atClassScope.add(code.supportingMembersLines());
        sourceFile.at(new Validate(typeName))
                  .add(validateBeforeBuild());
    }

    private static ImmutableList<String> wrapToMethod(ValidationConstraintCode generated,
                                                      MessageValidation validation) {
        var code = CodeBlock.builder();
        code.addStatement(newAccumulator());
        code.add(generated.codeBlock());
        code.add(extraInsertionPoint(validation.getType().getName()));
        code.add(generateValidationError());
        var validate = MethodSpec.methodBuilder(VALIDATE)
                .returns(OPTIONAL_ERROR)
                .addModifiers(PUBLIC)
                .addCode(code.build())
                .build();
        var methodLines = validate.toString().split(lineSeparator());
        return ImmutableList.copyOf(methodLines);
    }

    private static ImmutableList<String> validateBeforeBuild() {
        var result = new MessageReference("result");
        var code = CodeBlock.builder()
                .addStatement("$T error = $L", OPTIONAL_ERROR, new MethodCall(result, VALIDATE))
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
        var file = validation.getType().getFile();
        var typeName = validation.getType().getName();
        ImmutableList.Builder<CodeBlock> supportingMembers = ImmutableList.builder();
        for (var rule : validation.getRuleList()) {
            var context = new GenerationContext(
                    rule, msg, file, typeSystem, typeName, VIOLATIONS, this
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
