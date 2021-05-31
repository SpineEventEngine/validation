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

import io.spine.core.External;
import io.spine.core.Subscribe;
import io.spine.protodata.MessageType;
import io.spine.protodata.TypeEntered;
import io.spine.protodata.TypeName;
import io.spine.protodata.plugin.View;
import io.spine.validation.CompositeRuleAdded;
import io.spine.validation.MessageValidation;
import io.spine.validation.Rule;
import io.spine.validation.RuleAdded;

/**
 * A view which accumulates validation data for a message type.
 *
 * <p>To add more rules to the message validation, emit {@code RuleAdded} or
 * {@code CompositeRuleAdded} events.
 */
class MessageValidationView
        extends View<TypeName, MessageValidation, MessageValidation.Builder> {

    @Subscribe
    void on(@External TypeEntered event) {
        MessageType type = event.getType();
        builder().setName(type.getName())
                 .setType(type);
    }

    @Subscribe
    void on(RuleAdded event) {
        Rule roc = Rule
                .newBuilder()
                .setSimple(event.getRule())
                .buildPartial();
        builder().addRule(roc);
    }

    @Subscribe
    void on(CompositeRuleAdded event) {
        Rule roc = Rule
                .newBuilder()
                .setComposite(event.getRule())
                .buildPartial();
        builder().addRule(roc);
    }
}
