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

package io.spine.kanban.codegen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.CodeBlock;
import io.spine.protodata.File;
import io.spine.protodata.FilePath;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.language.CommonLanguages;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.renderer.SourceSet;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationException;
import io.spine.validation.MessageValidation;
import io.spine.validation.Rule;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import static io.spine.kanban.codegen.Poet.lines;
import static io.spine.protodata.Ast.javaFile;
import static io.spine.util.Exceptions.newIllegalArgumentException;

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
public final class JavaValidationRenderer extends Renderer {

    private static final int INDENT_LEVEL = 2;
    private static final String VIOLATIONS = "violations";

    private @MonotonicNonNull TypeSystem typeSystem;

    public JavaValidationRenderer() {
        super(ImmutableSet.of(CommonLanguages.getJava()));
    }

    @Override
    protected void doRender(SourceSet sources) {
        this.typeSystem = bakeTypeSystem();
        select(MessageValidation.class)
                .all()
                .stream()
                .filter(validation -> validation.getRuleCount() > 0)
                .forEach(validation -> {
                    File protoFile = findProtoFile(validation.getType().getFile());
                    Path javaFile = javaFile(validation.getType(), protoFile);
                    sources.file(javaFile)
                           .at(new Validate(validation.getName()))
                           .add(rulesToCode(validation), INDENT_LEVEL);
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

    private ImmutableList<String> rulesToCode(MessageValidation validation) {
        MessageReference result = new MessageReference("result");
        CodeBlock.Builder code = CodeBlock.builder();
        code.add(newAccumulator());
        code.add(generateValidationCode(validation, result));
        code.add(throwValidationException());
        return lines(code.build());
    }

    private static CodeBlock newAccumulator() {
        return CodeBlock.of("$T<$T> $N = new $T<>();",
                            ArrayList.class,
                            ConstraintViolation.class,
                            VIOLATIONS,
                            ArrayList.class);
    }

    private CodeBlock generateValidationCode(MessageValidation validation,
                                             MessageReference result) {
        CodeBlock.Builder code = CodeBlock.builder();
        for (Rule rule : validation.getRuleList()) {
            GenerationContext context = new GenerationContext(
                    rule, result, typeSystem, validation.getType().getName(), VIOLATIONS
            );
            JavaCodeGenerator generator = JavaCodeGeneration.generatorFor(context);
            CodeBlock block = generator.code();
            code.add(block);
        }
        return code.build();
    }

    private static CodeBlock throwValidationException() {
        CodeBlock.Builder code = CodeBlock.builder();
        code.beginControlFlow("if (!$N.isEmpty())", VIOLATIONS);
        code.addStatement("throw new $T($N)", ValidationException.class, VIOLATIONS);
        code.endControlFlow();
        return code.build();
    }
}
