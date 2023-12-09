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

package io.spine.validation;

import com.google.protobuf.BoolValue;
import io.spine.core.External;
import io.spine.core.Where;
import io.spine.protodata.FieldName;
import io.spine.protodata.File;
import io.spine.protodata.event.FieldOptionDiscovered;
import io.spine.protodata.TypeName;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.RuleAdded;
import io.spine.validation.event.SimpleRuleAdded;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protodata.Ast.isRepeated;
import static io.spine.protodata.Ast.getQualifiedName;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validation.EventFieldNames.OPTION_NAME;
import static io.spine.validation.SourceFiles.findField;

/**
 * A policy which, upon encountering a field with the {@code (distinct)} option, generates
 * a validation rule.
 *
 * <p>The validation rule prohibits duplicate entries in the associated field.
 */
final class DistinctPolicy extends ValidationPolicy<FieldOptionDiscovered> {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Duplicates in generated code.
    private static final String ERROR = "Collection must not contain duplicates.";

    @Override
    @React
    protected EitherOf2<RuleAdded, Nothing> whenever(
            @External @Where(field = OPTION_NAME, equals = "distinct") FieldOptionDiscovered event
    ) {
        var option = event.getOption();
        if (!unpack(option.getValue(), BoolValue.class).getValue()) {
            return withNothing();
        }
        checkCollection(event.getField(), event.getType(), event.getFile());
        var field = event.getField();
        var rule = SimpleRules.withCustom(
                field, DistinctCollection.getDefaultInstance(), ERROR, ERROR, false
        );
        return EitherOf2.withA(
                SimpleRuleAdded.newBuilder()
                        .setType(event.getType())
                        .setRule(rule)
                        .build()
        );
    }

    private void checkCollection(FieldName fieldName, TypeName typeName, File file) {
        var field = findField(fieldName, typeName, file, this);
        if (!isRepeated(field)) {
            throw newIllegalStateException(
                    "Field `%s.%s` is neither a `repeated` nor a `map` and " +
                            "therefore cannot be `distinct`.",
                    getQualifiedName(typeName),
                    fieldName.getValue());
        }
    }
}
