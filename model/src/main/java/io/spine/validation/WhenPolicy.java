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

import io.spine.core.External;
import io.spine.core.Where;
import io.spine.protodata.FieldOptionDiscovered;
import io.spine.protodata.Option;
import io.spine.protodata.plugin.Just;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.validation.event.SimpleRuleAdded;
import io.spine.protobuf.AnyPacker;
import io.spine.time.validation.TimeOption;
import io.spine.time.validation.Time;

import static io.spine.validation.EventFieldNames.OPTION_NAME;
import static io.spine.validation.SimpleRules.withCustom;
import static java.lang.String.format;

final class WhenPolicy extends Policy<FieldOptionDiscovered> {

    @Override
    @React
    protected Just<SimpleRuleAdded> whenever(
            @External @Where(field = OPTION_NAME, equals = "when") FieldOptionDiscovered event
    ) {
        Option option = event.getOption();
        TimeOption timeOption = AnyPacker.unpack(option.getValue(), TimeOption.class);
        Time time = timeOption.getIn();
        InTime feature = InTime.newBuilder()
                .setTime(time)
                .build();
        String errorMessage = format("The time must be in the %s.",
                                     time.name().toLowerCase());
        SimpleRule rule = withCustom(
                event.getField(),
                feature,
                errorMessage,
                errorMessage,
                true);
        return new Just<>(SimpleRuleAdded
                                  .newBuilder()
                                  .setType(event.getType())
                                  .setRule(rule)
                                  .build()
        );
    }
}

