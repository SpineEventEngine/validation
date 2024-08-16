/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import io.spine.base.EventMessage;
import io.spine.core.ContractFor;
import io.spine.protodata.plugin.Policy;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.validation.event.RuleAdded;

/**
 * A policy that reacts to an event with a {@link RuleAdded} event.
 *
 * <p>May ignore an event and return {@code Nothing} if necessary.
 *
 * @param <E>
 *         the type of the event to react to
 */
public abstract class ValidationPolicy<E extends EventMessage>
        extends Policy<E>
        implements ValidationPluginPart {

    @Override
    @ContractFor(handler = React.class)
    protected abstract EitherOf2<RuleAdded, Nothing> whenever(E event);

    /**
     * Creates an {@link EitherOf2} with {@code Nothing} in the {@code B} option.
     *
     * <p>Usage example:
     * <pre>
     *  {@literal class MyPolicy extends ValidationPolicy<TypeEntered>} {
     *      {@literal @Override @React}
     *      {@literal protected EitherOf2<RuleAdded, Nothing> whenever}(TypeEntered event) {
     *           if (!isRelevant(event)) {
     *               return withNothing();
     *           }
     *           return myCustomRule(event);
     *       }
     * </pre>
     */
    protected final EitherOf2<RuleAdded, Nothing> noReaction() {
        //TODO:2024-08-11:alexander.yevsyukov: Use EventProducer.noReaction() extension from `core-java`.
        return EitherOf2.withB(nothing());
    }
}
