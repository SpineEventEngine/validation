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

package io.spine.validation.java;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import io.spine.protodata.File;
import io.spine.protodata.FilePath;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.codegen.java.JavaRenderer;
import io.spine.protodata.codegen.java.Literal;
import io.spine.protodata.codegen.java.MessageReference;
import io.spine.protodata.codegen.java.MethodCall;
import io.spine.protodata.codegen.java.Poet;
import io.spine.protodata.codegen.java.This;
import io.spine.protodata.language.CommonLanguages;
import io.spine.protodata.language.Language;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.renderer.SourceSet;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import io.spine.validation.MessageValidation;
import io.spine.validation.Rule;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protodata.codegen.java.Ast2Java.javaFile;
import static io.spine.protodata.codegen.java.TypedInsertionPoint.CLASS_SCOPE;
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
 * <p>The generated code is a number of code lines. It does not contain declarations (clsses,
 * methods, etc.).
 *
 * <p>If the validation rules are broken, throws a {@link io.spine.validate.ValidationException}.
 */
@SuppressWarnings("unused") // Loaded by ProtoData via reflection.
public final class JavaValidationRenderer extends JavaRenderer {

    private static final Type OPTIONAL_ERROR =
            new TypeToken<Optional<ValidationError>>() {}.getType();

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
    private static final String VIOLATIONS = "violations";

    private @MonotonicNonNull TypeSystem typeSystem;

    @Override
    protected void render(SourceSet sources) {
        this.typeSystem = bakeTypeSystem();
        select(MessageValidation.class)
                .all()
                .stream()
                .filter(validation -> validation.getRuleCount() > 0)
                .forEach(validation -> {
                    File protoFile = findProtoFile(validation.getType().getFile());
                    Path javaFile = javaFile(validation.getType(), protoFile);
                    sources.file(javaFile)
                           .at(CLASS_SCOPE.forType(validation.getName()))
                           .withExtraIndentation(INDENT_LEVEL)
                           .add(rulesToMethod(validation));
                    sources.file(javaFile)
                           .at(new Validate(validation.getName()))
                           .withExtraIndentation(INDENT_LEVEL)
                           .add(validateBeforeBuild());
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

    private TypeSystem bakeTypeSystem() {
        Set<ProtobufSourceFile> files = select(ProtobufSourceFile.class).all();
        TypeSystem.Builder types = TypeSystem.newBuilder();
        for (ProtobufSourceFile file : files) {
            file.getTypeMap().values().forEach(type -> types.put(file.getFile(), type));
            file.getEnumTypeMap().values().forEach(type -> types.put(file.getFile(), type));
        }
        return types.build();
    }

    private ImmutableList<String> rulesToMethod(MessageValidation validation) {
        MessageReference result = This.INSTANCE.asMessage();
        CodeBlock.Builder code = CodeBlock.builder();
        code.addStatement(newAccumulator());
        code.add(generateValidationCode(validation, result));
        code.add(extraInsertionPoint(validation.getType().getName()));
        code.add(generateValidationError());
        MethodSpec validate = MethodSpec
                .methodBuilder(VALIDATE)
                .returns(OPTIONAL_ERROR)
                .addModifiers(PUBLIC)
                .addCode(code.build())
                .build();
        return ImmutableList.of(validate.toString());
    }

    private static ImmutableList<String> validateBeforeBuild() {
        MessageReference result = new MessageReference("result");
        CodeBlock code = CodeBlock
                .builder()
                .addStatement("$T error = $L", OPTIONAL_ERROR, new MethodCall(result, VALIDATE))
                .beginControlFlow("if (error.isPresent())")
                .addStatement("throw new $T(error.get().getConstraintViolationList())", ValidationException.class)
                .endControlFlow()
                .build();
        return Poet.lines(code);
    }

    private static CodeBlock newAccumulator() {
        return CodeBlock.of("$T<$T> $N = new $T<>()",
                            ArrayList.class,
                            ConstraintViolation.class,
                            VIOLATIONS,
                            ArrayList.class);
    }

    private CodeBlock generateValidationCode(MessageValidation validation,
                                             MessageReference result) {
        CodeBlock.Builder code = CodeBlock.builder();
        FilePath file = validation.getType().getFile();
        TypeName typeName = validation.getType().getName();
        for (Rule rule : validation.getRuleList()) {
            GenerationContext context = new GenerationContext(
                    rule, result, file, typeSystem, typeName, VIOLATIONS, this
            );
            CodeGenerator generator = JavaCodeGeneration.generatorFor(context);
            CodeBlock block = generator.code();
            code.add(block);
        }
        return code.build();
    }

    private static CodeBlock extraInsertionPoint(TypeName name) {
        InsertionPoint insertionPoint = new ExtraValidation(name);
        Language java = CommonLanguages.java();
        String line = java.comment(getCodeLine(insertionPoint)) + lineSeparator();
        return CodeBlock.of(line);
    }

    private static CodeBlock generateValidationError() {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("if (!$N.isEmpty())", VIOLATIONS);
        MethodCall errorBuilder = new ClassName(ValidationError.class)
                .newBuilder()
                .chainAddAll("constraint_violation", new Literal(VIOLATIONS))
                .chainBuild();
        MethodCall newOptional = new ClassName(Optional.class)
                .call("of", newArrayList(errorBuilder), newArrayList());
        code.addStatement(RETURN_LITERAL, newOptional);
        code.nextControlFlow("else");
        MethodCall optionalEmpty = new ClassName(Optional.class)
                .call("empty", newArrayList(), newArrayList());
        code.addStatement(RETURN_LITERAL, optionalEmpty);
        code.endControlFlow();
        return code.build();
    }
}
