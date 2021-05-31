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

package io.spine.validation;

import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Extension;
import io.spine.core.External;
import io.spine.option.OptionsProto;
import io.spine.protodata.Ast;
import io.spine.protodata.Field;
import io.spine.protodata.FieldOptionDiscovered;
import io.spine.protodata.MessageType;
import io.spine.protodata.Option;
import io.spine.protodata.ProtobufSourceFile;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.util.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Policy} which controls whether or not a field should be validated as {@code required}.
 *
 * <p>Whenever a field option is discovered, if that option is the {@code required} option, and
 * the value is {@code true}, and the field type supports such validation, a validation rule
 * is added. If any of these conditions are not met, nothing happens.
 */
public final class RequiredRulePolicy extends Policy<FieldOptionDiscovered> {

    @NotNull
    @Override
    @React
    public EitherOf2<RuleAdded, Nothing> whenever(@External FieldOptionDiscovered event) {
        Option option = event.getOption();
        if (isOption(option, OptionsProto.required)) {
            ProtobufSourceFile file = select(ProtobufSourceFile.class)
                    .withId(event.getFile())
                    .orElseThrow(() -> Exceptions.newIllegalArgumentException(
                            "Unknown file `%s`.", event.getFile()
                                                       .getValue()
                    ));
            MessageType type = file.getTypeMap()
                                   .get(Ast.typeUrl(event.getType()));
            Field field = type.getFieldList()
                              .stream()
                              .filter(f -> f.getName().equals(event.getField()))
                              .findAny()
                              .orElseThrow(() -> Exceptions.newIllegalArgumentException(
                                      "Unknown field `%s`.", event.getField()
                              ));
            return EitherOf2.withA(requiredRule(field));
        }
        return EitherOf2.withB(nothing());
    }

    private static RuleAdded requiredRule(Field field) {
        Value defaultValue = DefaultValue.forField(field);
        @SuppressWarnings("DuplicateStringLiteralInspection") // Duplication in generated code.
        SimpleRule rule = SimpleRule.newBuilder()
                                    .setErrorMessage("Field must be set.")
                                    .setField(field)
                                    .setSign(ComparisonOperator.NOT_EQUAL)
                                    .setOtherValue(defaultValue)
                                    .vBuild();
        return RuleAdded.newBuilder()
                        .setType(field.getDeclaringType())
                        .setRule(rule)
                        .vBuild();
    }

    private static boolean isOption(Option option, Extension<FieldOptions, ?> extension) {
        FieldDescriptor descriptor = extension.getDescriptor();
        return option.getName().equals(descriptor.getName())
                && option.getNumber() == extension.getNumber();
    }
}
