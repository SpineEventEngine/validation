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

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import io.spine.core.External;
import io.spine.option.PatternOption;
import io.spine.protodata.FieldOptionDiscovered;
import io.spine.protodata.Option;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.SimpleRuleAdded;

import static io.spine.option.OptionsProto.pattern;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validation.Options.is;
import static java.lang.String.format;

/**
 * A policy to add a validation rule to a type whenever the {@code (pattern)} field option
 * is discovered.
 */
final class PatternPolicy extends Policy<FieldOptionDiscovered> {

    private static final Escaper slashEscaper = Escapers
            .builder()
            .addEscape('\\',"\\\\")
            .build();

    @Override
    @React
    public EitherOf2<SimpleRuleAdded, Nothing> whenever(@External FieldOptionDiscovered event) {
        Option option = event.getOption();
        if (!is(option, pattern)) {
            return EitherOf2.withB(nothing());
        }
        PatternOption optionValue = unpack(option.getValue(), PatternOption.class);
        String regex = optionValue.getRegex();
        Regex feature = Regex.newBuilder()
                .setPattern(regex)
                .setModifier(optionValue.getModifier())
                .build();
        String error = format("The string must match the regular expression `%s`.",
                              slashEscaper.escape(regex));
        SimpleRule rule = SimpleRules.withCustom(
                event.getField(),
                feature,
                "String should match regex.",
                error,
                true
        );
        return EitherOf2.withA(
                SimpleRuleAdded.newBuilder()
                        .setType(event.getType())
                        .setRule(rule)
                        .build()
        );
    }
}
