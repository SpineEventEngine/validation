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

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import io.spine.core.External;
import io.spine.core.Where;
import io.spine.option.PatternOption;
import io.spine.protodata.event.FieldOptionDiscovered;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.Just;
import io.spine.server.event.React;
import io.spine.string.CharSequences;
import io.spine.validation.event.SimpleRuleAdded;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.event.Just.just;
import static io.spine.string.CharSequences.containsLineSeparators;
import static io.spine.validate.Diags.Regex.errorMessage;
import static io.spine.validation.EventFieldNames.OPTION_NAME;

/**
 * A policy to add a validation rule to a type whenever the {@code (pattern)} field option
 * is discovered.
 */
final class PatternPolicy extends Policy<FieldOptionDiscovered> {

    @Override
    @React
    protected Just<SimpleRuleAdded> whenever(
            @External @Where(field = OPTION_NAME, equals = "pattern") FieldOptionDiscovered event
    ) {
        var option = event.getOption();
        var optionValue = unpack(option.getValue(), PatternOption.class);
        var regex = optionValue.getRegex();
        var feature = Regex.newBuilder()
                .setPattern(regex)
                .setModifier(optionValue.getModifier())
                .build();
        var customError = optionValue.getErrorMsg();
        var error = customError.isEmpty()
                       ? ErrorMessage.notMatching(regex)
                       : customError;
        var rule = SimpleRules.withCustom(
                event.getField(),
                feature,
                "String should match regex.",
                error,
                true
        );
        return just(
                SimpleRuleAdded.newBuilder()
                        .setType(event.getType())
                        .setRule(rule)
                        .build()
        );
    }

    /**
     * Creates an error message for a value not matching a given regex.
     */
    private static final class ErrorMessage {

        private static final Escaper slashEscaper = Escapers.builder()
                .addEscape('\\', "\\\\")
                .build();

        private static String notMatching(String regex) {
            var withoutLineSeparators = escapeLineSeparators(regex);
            return errorMessage(slashEscaper.escape(withoutLineSeparators));
        }

        private static String escapeLineSeparators(String regex) {
            var toEscape = regex;
            if (containsLineSeparators(regex)) {
                toEscape = CharSequences.escapeLineSeparators(regex);
            }
            return toEscape;
        }
    }
}
