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

import io.spine.protodata.File;
import io.spine.protodata.FilePath;
import io.spine.protodata.MessageType;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.codegen.java.JavaRenderer;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.renderer.SourceFile;
import io.spine.protodata.renderer.SourceFileSet;
import io.spine.validation.MessageValidation;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.codegen.java.Ast2Java.javaFile;
import static io.spine.util.Exceptions.newIllegalArgumentException;

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
        this.typeSystem = TypeSystem.assemble(this);
        this.validations = findValidations();
        var messageTypes = queryMessageTypes();
        messageTypes.forEach(this::generateCode);
    }

    TypeSystem typeSystem() {
        return checkNotNull(typeSystem);
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

        var validationCode = new ValidationCode(this, validation, sourceFile);
        validationCode.generate();
    }
}
